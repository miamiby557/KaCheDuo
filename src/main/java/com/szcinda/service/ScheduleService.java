package com.szcinda.service;

import com.szcinda.repository.*;
import com.szcinda.service.robotTask.CreateRobotTaskDto;
import com.szcinda.service.robotTask.RobotTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Component
public class ScheduleService {

    private final static Logger logger = LoggerFactory.getLogger(ScheduleService.class);

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

    // ip 和机器人账号对应表
    public static ConcurrentHashMap<String, List<String>> ipRobotList = new ConcurrentHashMap<>();

    // 需要重启的IP机器
    public static Set<String> ipList = new HashSet<>();


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
    @Scheduled(cron = "0 0 2,10,16 * * ?")
    public void run() throws Exception {
        // 清空历史任务
        robotSearchLocationList.clear();
        List<Robot> robots = robotRepository.findByType(TypeStringUtils.robotType3);
        if (robots.size() > 0) {
            for (Robot robot : robots) {
                if (robot.isRunLocation()) {
                    //找处理、位置监控账号来处理位置监控
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


    // 刷新机器人在线的列表，如果5分钟内没有发送日志，则表示不在线
    @Scheduled(cron = "0/30 * * * * ?")
    public void checkAlive() throws Exception {
        List<String> expireList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        robotAliveMap.forEach((phone, dto) -> {
            LocalDateTime time = dto.getTime();
            Duration duration = Duration.between(now, time);
            long minutes = Math.abs(duration.toMinutes());//相差的分钟数
            logger.info(String.format("该账号【%s】上一次心跳时间：【%s】", phone, time.toString()));
            if (minutes >= 5) {
                logger.info(String.format("该账号【%s】判断为下线", phone));
                expireList.add(phone);
            }
        });
        // 删除过期的账号
        if (expireList.size() > 0) {
            logger.info(String.format("以下账号被删除活动状态：【%s】", expireList.toString()));
            for (String phone : expireList) {
                robotAliveMap.remove(phone);
            }
        }
        // 删除15分钟没有发送心跳的监控帐号
        List<String> deleteIds = new ArrayList<>();

        mainRobotWatchMap.forEach((id, time) -> {
            Duration duration = Duration.between(now, time);
            long minutes = Math.abs(duration.toMinutes());//相差的分钟数
            copyOnWriteRobots.stream().filter(robot -> robot.getId().equals(id))
                    .findFirst().ifPresent(robot -> {
                logger.info(String.format("该监控账号【%s】上一次心跳时间：【%s】", robot.getPhone(), time.toString()));
                if (minutes >= 5) {
                    logger.info(String.format("该监控账号【%s】判断为下线", robot.getPhone()));
                    deleteIds.add(id);
                }
            });
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

    /**
     * 2022年5月20号正式修改为直接发送到卡车多APP
     */
    // 每天9点发送司机前一天违规内容
//    @Scheduled(cron = "0 0 9 * * ?")
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


    //    @Scheduled(cron = "0 0 1 * * ?")
    public void sendToApp() {
        // 查询昨天的数据
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
        // 组装发送数据
        listMap.forEach((vehicleNo, dataList) -> {
            PostAppDataDto appDataDto = new PostAppDataDto();
            appDataDto.setVehicleNo(vehicleNo);
            drivers.stream().filter(driver -> driver.getVehicleNo().equals(vehicleNo))
                    .findFirst()
                    .ifPresent(driver -> {
                        appDataDto.setCompany(driver.getCompany());
                        appDataDto.setPhone(driver.getPhone());
                    });
            List<PostAppDataDto.Item> items = new ArrayList<>();

            for (FengXian fengXian : dataList) {
                PostAppDataDto.Item item = new PostAppDataDto.Item();
                item.setId(fengXian.getId());
                item.setVehicleColor(fengXian.getVehicleColor());
                item.setArea(fengXian.getArea());
                item.setCurrentDriver(fengXian.getCurrentDriver());
                item.setThirdOrg(fengXian.getThirdOrg());
                item.setBusinessScope(fengXian.getBusinessScope());
                item.setDangerType(fengXian.getDangerType());
                item.setDangerLevel(fengXian.getDangerLevel());
                item.setSpeed(fengXian.getSpeed());
                item.setHappenPlace(fengXian.getHappenPlace());
                item.setHappenTime(fengXian.getHappenTime());
                if (fengXian.getGdCreateTime() != null) {
                    item.setGdCreateTime(fengXian.getGdCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                }
                items.add(item);
            }
            appDataDto.setDangerList(items);
            HttpUtil.post(appDataDto);
        });
    }

    // 20分钟执行一次检查，如果发现掉线超过10分钟，则代表已经下线
    @Scheduled(cron = "0 */15 * * * ?")
    public void checkRobotIsAliveAndSendMsgToAdmin() {
        List<Robot> robots = robotRepository.findByParentIdIsNull();
        // 判断：如果监控账号的心跳数量不等于正常机器人的数量，则代表有机器人下线了，需要重启
        long runRobotCount = robots.stream().filter(Robot::isRun).count();
        if (runRobotCount > mainRobotWatchMap.size()) {
            ipList.addAll(ipRobotList.keySet());
        }
        if (mainRobotWatchMap.size() == 0) {
            String content = "注意：全部监控端账号都下线了，请及时查看运行程序状态";
            String[] strings = wechats.split(",");
            for (String wechat : strings) {
                // 判断是否存在报警记录，存在则更新
                List<ScreenShotTask> screenShotTasks = screenShotTaskRepository.findByWechatAndStatus(wechat, TypeStringUtils.wechat_status5);
                if (screenShotTasks.size() > 0) {
                    ScreenShotTask screenShotTask = screenShotTasks.get(0);
                    screenShotTask.setContent(content);
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
                    screenShotTask.setContent(content);
                    screenShotTaskRepository.save(screenShotTask);
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

    // 判断IP是否需要重启
    public boolean needReboot(String ip) {
        return ipList.contains(ip);
    }

    // 重启所有机器
    public void rebootAllIp() {
        ipList.addAll(ipRobotList.keySet());
    }

    // 重启成功后剔除IP
    public void rebootSuccess(String ip) {
        ipList.remove(ip);
    }

    public void aliveIp(String ip, String account) {
        if (ipRobotList.containsKey(ip)) {
            List<String> accountList = ipRobotList.get(ip);
            if (accountList == null) {
                accountList = new ArrayList<>();
                ipRobotList.put(ip, accountList);
            } else if (!accountList.contains(account)) {
                accountList.add(account);
            }
        } else {
            List<String> accountList = new ArrayList<>();
            accountList.add(account);
            ipRobotList.put(ip, accountList);
        }
    }
}
