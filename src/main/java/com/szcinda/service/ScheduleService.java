package com.szcinda.service;

import com.szcinda.repository.*;
import com.szcinda.service.robotTask.CreateRobotTaskDto;
import com.szcinda.service.robotTask.RobotTaskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Component
public class ScheduleService {


    private final RobotRepository robotRepository;
    private final RobotTaskService robotTaskService;
    private final RobotTaskRepository robotTaskRepository;
    private final WorkRobotRepository workRobotRepository;
    private final FengXianRepository fengXianRepository;
    private final DriverRepository driverRepository;
    private final SnowFlakeFactory snowFlakeFactory = SnowFlakeFactory.getInstance();
    private final ScreenShotTaskRepository screenShotTaskRepository;
    private final WechatRepository wechatRepository;

    // 管理员微信号
    @Value("${admin.user.wechat}")
    private String wechats;

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

    // 需要同步好友列表的微信
    public static CopyOnWriteArrayList<String> needSyncFriendWechatList = new CopyOnWriteArrayList<>();

    public ScheduleService(RobotRepository robotRepository, RobotTaskService robotTaskService,
                           RobotTaskRepository robotTaskRepository, WorkRobotRepository workRobotRepository, FengXianRepository fengXianRepository,
                           DriverRepository driverRepository, ScreenShotTaskRepository screenShotTaskRepository, WechatRepository wechatRepository) {
        this.robotRepository = robotRepository;
        this.robotTaskService = robotTaskService;
        this.robotTaskRepository = robotTaskRepository;
        this.workRobotRepository = workRobotRepository;
        this.fengXianRepository = fengXianRepository;
        this.driverRepository = driverRepository;
        this.screenShotTaskRepository = screenShotTaskRepository;
        this.wechatRepository = wechatRepository;
    }

    // 每日0时、8时、16时循环一次
    @Scheduled(cron = "0 0 0,8,16 * * ?")
    public void run() throws Exception {
        // 清空历史任务
        robotSearchLocationList.clear();
        List<Robot> robots = robotRepository.findByType(TypeStringUtils.robotType3);
        if (robots.size() > 0) {
            for (Robot robot : robots) {
                if (robot.isRunLocation()) {
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
        needSyncFriendWechatList.clear();
        List<Wechat> wechats = wechatRepository.findAll();
        for (Wechat wechat : wechats) {
            needSyncFriendWechatList.add(wechat.getNo());
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

    // 检查机器人是否在线
    public boolean isAlive(String userName) {
        return robotAliveMap.containsKey(userName);
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

    // 每天9点发送司机前一天违规内容
    @Scheduled(cron = "0 0 9 * * ?")
    public void sendMsgToDriver() {
        LocalDate lastDate = LocalDate.now().minusDays(1);
        Specification<FengXian> specification = ((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Predicate createTimeStart = criteriaBuilder.greaterThanOrEqualTo(root.get("createTime"), lastDate.atStartOfDay());
            predicates.add(createTimeStart);
            Predicate createTimeEnd = criteriaBuilder.lessThan(root.get("createTime"), lastDate.plusDays(1).atStartOfDay());
            predicates.add(createTimeEnd);
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        List<FengXian> fengXians = fengXianRepository.findAll(specification);
        // 按照车牌进行分组
        Map<String, List<FengXian>> listMap = fengXians.stream().collect(Collectors.groupingBy(FengXian::getVehicleNo));
        Set<String> vehicleList = listMap.keySet();
        List<Driver> drivers = driverRepository.findByVehicleNoIn(vehicleList);
        listMap.forEach((vehicleNo, list) -> {
            drivers.stream().filter(driver -> vehicleNo.equals(driver.getVehicleNo()) && StringUtils.hasText(driver.getWechat()))
                    .findFirst()
                    .ifPresent(driver -> {
                        // 把消息汇总成一条发送
                        List<String> msgList = new ArrayList<>();
                        StringBuilder stringBuilder = new StringBuilder(String.format("卡车多物流科技提醒您，【%s】昨天存在以下违规内容：", vehicleNo));
                        msgList.add(stringBuilder.toString());
                        int index = 1;
                        for (FengXian fengXian : list) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("第").append(index).append("条违规：").append("地点【").append(fengXian.getHappenPlace())
                                    .append("】违规时间【").append(fengXian.getHappenTime()).append("】违规类型【").append(fengXian.getDangerType())
                                    .append("】");
                            msgList.add(stringBuilder.toString());
                            index++;
                        }
                        stringBuilder = new StringBuilder("请注意行车规范，杜绝此类行为再次发生。请您收到回复”确认“！");
                        msgList.add(stringBuilder.toString());
                        String totalMsg = String.join("\n", msgList);
                        ScreenShotTask screenShotTask = new ScreenShotTask();
                        screenShotTask.setId(snowFlakeFactory.nextId("ST"));
                        screenShotTask.setWechat(driver.getWechat());
                        screenShotTask.setVehicleNo(vehicleNo);
                        screenShotTask.setOwnerWechat(driver.getOwnerWechat());
                        screenShotTask.setWxid(driver.getWxid());
                        screenShotTask.setOwner(list.get(0).getOwner());
                        screenShotTask.setStatus(TypeStringUtils.wechat_status3);
                        screenShotTask.setContent(totalMsg);
                        screenShotTaskRepository.save(screenShotTask);
                    });
        });
    }

    // 30分钟执行一次检查，如果发现掉线超过15分钟，则代表已经下线
    @Scheduled(cron = "0 */30 * * * ?")
    public void checkRobotIsAliveAndSendMsgToAdmin() {
        List<Robot> robots = robotRepository.findByParentIdIsNull();
        LocalDateTime now = LocalDateTime.now();
        StringBuilder stringBuilder = new StringBuilder("机器人下线情况：").append("\n");
        int index = 1;
        boolean hasDown = false;
        for (Robot robot : robots) {
            if (!robot.isRun()) {
                continue;
            }
            if (robotAliveMap.containsKey(robot.getPhone())) {
                RobotAliveDto aliveDto = robotAliveMap.get(robot.getPhone());
                Duration duration = Duration.between(now, aliveDto.getTime());
                long minutes = Math.abs(duration.toMinutes());//相差的分钟数
                if (minutes >= 15) {
                    // 代表下线
                    stringBuilder.append("第").append(index).append("个账号：").append(robot.getPhone())
                            .append("(").append(robot.getCompany()).append(")").append("\n");
                    index++;
                    hasDown = true;
                }
            } else {
                stringBuilder.append("第").append(index).append("个账号：").append(robot.getPhone())
                        .append("(").append(robot.getCompany()).append(")").append("\n");
                index++;
                hasDown = true;
            }
        }
        if (hasDown) {
            // 需要创建一条微信发消息任务通知管理员
            if (StringUtils.hasText(wechats)) {
                String[] strings = wechats.split(",");
                for (String wechat : strings) {
                    // 判断是否存在报警记录，存在则更新
                    List<ScreenShotTask> screenShotTasks = screenShotTaskRepository.findByWechatAndStatus(wechat, TypeStringUtils.wechat_status5);
                    if (screenShotTasks.size() > 0) {
                        ScreenShotTask screenShotTask = screenShotTasks.get(0);
                        screenShotTask.setContent(stringBuilder.toString());
                        screenShotTaskRepository.save(screenShotTask);
                    } else {
                        ScreenShotTask screenShotTask = new ScreenShotTask();
                        screenShotTask.setId(snowFlakeFactory.nextId("ST"));
                        screenShotTask.setWechat(wechat);
                        screenShotTask.setVehicleNo("");
                        screenShotTask.setOwnerWechat("anqin1588");
                        screenShotTask.setWxid(wechat);
                        screenShotTask.setOwner("");
                        screenShotTask.setStatus(TypeStringUtils.wechat_status5);
                        screenShotTask.setContent(stringBuilder.toString());
                        screenShotTaskRepository.save(screenShotTask);
                    }
                }
            }
        }
        // 检查是否有超过30条处置任务，说明可能处理端机器人有问题了
        List<RobotTask> all = robotTaskRepository.findAll();
        int size = all.size();
        if (size >= 30) {
            if (StringUtils.hasText(wechats)) {
                String[] strings = wechats.split(",");
                for (String wechat : strings) {
                    // 判断是否存在报警记录，存在则更新
                    List<ScreenShotTask> screenShotTasks = screenShotTaskRepository.findByWechatAndStatus(wechat, TypeStringUtils.wechat_status5);
                    if (screenShotTasks.size() > 0) {
                        ScreenShotTask screenShotTask = screenShotTasks.get(0);
                        screenShotTask.setContent(String.format("目前正在运行的任务列表已经堆积了【%d】条任务没有处置或处理，请赶紧查看机器人所在机器的运行情况", size));
                        screenShotTaskRepository.save(screenShotTask);
                    } else {
                        ScreenShotTask screenShotTask = new ScreenShotTask();
                        screenShotTask.setId(snowFlakeFactory.nextId("ST"));
                        screenShotTask.setWechat(wechat);
                        screenShotTask.setVehicleNo("");
                        screenShotTask.setOwnerWechat("anqin1588");
                        screenShotTask.setWxid(wechat);
                        screenShotTask.setOwner("");
                        screenShotTask.setStatus(TypeStringUtils.wechat_status5);
                        screenShotTask.setContent(String.format("目前正在运行的任务列表已经堆积了【%d】条任务没有处置或处理，请赶紧查看机器人所在机器的运行情况", size));
                        screenShotTaskRepository.save(screenShotTask);
                    }
                }
            }
        }
    }

    public boolean canRunSyncFriends(String wechat) {
        if (needSyncFriendWechatList.contains(wechat)) {
            needSyncFriendWechatList.remove(wechat);
            return true;
        }
        return false;
    }
}
