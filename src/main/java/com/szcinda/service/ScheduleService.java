package com.szcinda.service;

import com.szcinda.repository.Robot;
import com.szcinda.repository.RobotRepository;
import com.szcinda.repository.WorkRobot;
import com.szcinda.repository.WorkRobotRepository;
import com.szcinda.service.robotTask.CreateRobotTaskDto;
import com.szcinda.service.robotTask.RobotTaskService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class ScheduleService {


    private final RobotRepository robotRepository;
    private final RobotTaskService robotTaskService;
    private final WorkRobotRepository workRobotRepository;

    // 需要运行位置监控的机器人名称列表
    public static List<String> robotSearchLocationList = new ArrayList<>();

    // 机器人在线
    public static ConcurrentHashMap<String, RobotAliveDto> robotAliveMap = new ConcurrentHashMap<>();


    // 需要运行处置的机器人列表
    public static ConcurrentHashMap<String, Boolean> robotChuZhiMap = new ConcurrentHashMap<>();

    // 正在监控的主账号列表 如果帐号等监控中，就加入到此队列
    public static ConcurrentHashMap<String, LocalDateTime> mainRobotWatchMap = new ConcurrentHashMap<>();

    // 机器人帐号密码
    public static ConcurrentHashMap<String, String> robotPwdMap = new ConcurrentHashMap<>();

    // 系统所有登录帐号的列表，避免前端的定时器每次请求都到数据库 CopyOnWriteArrayList适合读多写少的并发场景
    public static CopyOnWriteArrayList<Robot> copyOnWriteRobots = new CopyOnWriteArrayList<>();

    public ScheduleService(RobotRepository robotRepository, RobotTaskService robotTaskService, WorkRobotRepository workRobotRepository) {
        this.robotRepository = robotRepository;
        this.robotTaskService = robotTaskService;
        this.workRobotRepository = workRobotRepository;
    }

    // 每日0时、8时、16时循环一次
    @Scheduled(cron = "0 0 0,8,16 * * ?")
    public void run() throws Exception {
        // 清空历史任务
        robotSearchLocationList.clear();
        List<Robot> robots = robotRepository.findByType(TypeStringUtils.robotType3);
        if (robots.size() > 0) {
            for (Robot robot : robots) {
                //找处理、位置监控账号来处理位置监控的上传
                robotSearchLocationList.add(robot.getPhone());
                robotPwdMap.put(robot.getPhone(), robot.getPwd());
                if (robot.isRun()) {
                    CreateRobotTaskDto taskDto = new CreateRobotTaskDto();
                    taskDto.setTaskType(TypeStringUtils.robotType3);
                    taskDto.setUserName(robot.getPhone());
                    taskDto.setPwd(robot.getPwd());
                    taskDto.setCompany(robot.getCompany());
                    robotTaskService.create(taskDto);
                }
            }
        }
    }

    public void refreshRobots() {
        List<Robot> robots = robotRepository.findAll();
        copyOnWriteRobots.addAll(robots);
    }

    public List<Robot> queryAllRobotsFromCopyOnWriteRobots() {
        if (copyOnWriteRobots.size() == 0) {
            refreshRobots();
        }
        return new ArrayList<>(copyOnWriteRobots);
    }


    public List<Robot> queryBySelfFromCopyOnWriteRobots(String owner) {
        if (copyOnWriteRobots.size() == 0) {
            refreshRobots();
        }
        List<Robot> robots = new ArrayList<>();
        for (Robot copyOnWriteRobot : copyOnWriteRobots) {
            if (copyOnWriteRobot.getOwner().equals(owner) && StringUtils.isEmpty(copyOnWriteRobot.getParentId())) {
                robots.add(copyOnWriteRobot);
            }
        }
        return robots;
    }

    public List<Robot> querySubRobotsFromCopyOnWriteRobots(String parentId) {
        List<Robot> robots = new ArrayList<>();
        for (Robot copyOnWriteRobot : copyOnWriteRobots) {
            if (copyOnWriteRobot.getParentId() != null && copyOnWriteRobot.getParentId().equals(parentId)) {
                robots.add(copyOnWriteRobot);
            }
        }
        return robots;
    }


    public void removeRobotFromCopyOnWriteRobots(String id) {
        copyOnWriteRobots.removeIf(copyOnWriteRobot -> copyOnWriteRobot.getId().equals(id));
    }

    public void addRobotFromCopyOnWriteRobots(Robot robot) {
        // 判断是否存在
        boolean hasData = false;
        for (Robot copyOnWriteRobot : copyOnWriteRobots) {
            if (copyOnWriteRobot.getId().equals(robot.getId())) {
                hasData = true;
                break;
            }
        }
        if (!hasData)
            copyOnWriteRobots.add(robot);
    }

    public void updateRobotFromCopyOnWriteRobots(Robot robot) {
        for (Robot copyOnWriteRobot : copyOnWriteRobots) {
            if (copyOnWriteRobot.getId().equals(robot.getId())) {
                copyOnWriteRobot.setPhone(robot.getPhone());
                copyOnWriteRobot.setPwd(robot.getPwd());
                copyOnWriteRobot.setAccount2(robot.getAccount2());
                copyOnWriteRobot.setPwd2(robot.getPwd2());
                copyOnWriteRobot.setRun(robot.isRun());
            }
        }
    }


    // 刷新机器人在线的列表，如果15分钟内没有发送日志，则表示不在线
    @Scheduled(cron = "0/30 * * * * ?")
    public void checkAlive() throws Exception {
        List<String> expireList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        robotAliveMap.forEach((phone, dto) -> {
            LocalDateTime time = dto.getTime();
            Duration duration = Duration.between(now, time);
            long minutes = Math.abs(duration.toMinutes());//相差的分钟数
            if (minutes >= 2) {
                expireList.add(phone);
            }
        });
        // 删除过期的账号
        if (expireList.size() > 0) {
            for (String phone : expireList) {
                robotAliveMap.remove(phone);
            }
        }
        // 删除15分钟没有发送心跳的监控帐号
        List<String> deleteIds = new ArrayList<>();
        mainRobotWatchMap.forEach((id, time) -> {
            Duration duration = Duration.between(now, time);
            long minutes = Math.abs(duration.toMinutes());//相差的分钟数
            if (minutes >= 2) {
                deleteIds.add(id);
            }
        });
        if (deleteIds.size() > 0) {
            for (String deleteId : deleteIds) {
                mainRobotWatchMap.remove(deleteId);
            }
        }
    }

    // 添加到或者更新到主账号监控列表
    public void updateToMainRobotWatchMap(String id) {
        mainRobotWatchMap.put(id, LocalDateTime.now());
    }

    // 检查是否在主账号监控集合
    public boolean checkIsInMainRobotWatchMap(String id) {
        return mainRobotWatchMap.containsKey(id);
    }

    // 从主账号集合中删除
    public void removeFromMainRobotWatchMap(String id) {
        mainRobotWatchMap.remove(id);
    }

    // 判断手机号是否在列表里面，如果在，就需要运行一次位置监控流程
    public boolean isInList(String phone) {
        boolean isIn = robotSearchLocationList.contains(phone);
        if (isIn) {
            // 清除记录，避免重复运行
            robotSearchLocationList.remove(phone);
        }
        return isIn;
    }

    public void changeChuZhiRobotStatus(String phone, boolean status) {
        robotChuZhiMap.put(phone, status);
    }


    public boolean canRunFX(String phone) {
        if (robotChuZhiMap.containsKey(phone)) {
            return robotChuZhiMap.get(phone);
        } else {
            Robot robot = robotRepository.findByPhone(phone);
            if (robot != null) {
                robotChuZhiMap.put(robot.getPhone(), robot.isRun());
                return robot.isRun();
            }
        }
        return false;
    }

    public boolean canRunChuLiAndLocation(String phone) {
//        if (robotChuZhiMap.containsKey(phone)) {
//            return robotChuZhiMap.get(phone);
//        } else {
//            Robot robot = robotRepository.findByPhone(phone);
//            if (robot != null) {
//                robotChuZhiMap.put(robot.getPhone(), robot.isRun());
//                return robot.isRun();
//            }
//        }
//        return false;
        WorkRobot workRobot = workRobotRepository.findByUserName(phone);
        return workRobot == null;
    }

    public String getPwd(String phone) {
        if (robotPwdMap.containsKey(phone)) {
            return robotPwdMap.get(phone);
        } else {
            Robot robot = robotRepository.findByPhone(phone);
            robotPwdMap.put(robot.getPhone(), robot.getPwd());
            return robotPwdMap.get(phone);
        }
    }


    public void alive(String id, String phone) {
        RobotAliveDto dto;
        if (robotAliveMap.containsKey(phone)) {
            dto = robotAliveMap.get(phone);
            if (dto == null) {
                dto = new RobotAliveDto();
            }
            dto.setTime(LocalDateTime.now());
        } else {
            dto = new RobotAliveDto();
            dto.setTime(LocalDateTime.now());
        }
        //添加到机器人在线集合中
        robotAliveMap.put(phone, dto);
        // 添加到主账号监控集合中
        if (mainRobotWatchMap.containsKey(id)) {
            mainRobotWatchMap.put(id, LocalDateTime.now());
        }
    }

    public boolean canWatch(String id) {
        return mainRobotWatchMap.containsKey(id);
    }
}
