package com.szcinda.service;

import com.szcinda.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class RobotScheduleService {


    private final static Logger logger = LoggerFactory.getLogger(RobotScheduleService.class);

    //存放监控帐号的优先级排序列表
    public static final CopyOnWriteArrayList<RobotPriorityDto> watchRobotList = new CopyOnWriteArrayList<>();

    //存在查岗帐号优先级的排序列表
    public static final CopyOnWriteArrayList<RobotPriorityDto> chaGangRobotList = new CopyOnWriteArrayList<>();

    private final RobotRepository robotRepository;
    private final ChaGangRepository chaGangRepository;

    public RobotScheduleService(RobotRepository robotRepository, ChaGangRepository chaGangRepository) {
        this.robotRepository = robotRepository;
        this.chaGangRepository = chaGangRepository;
    }

    public String getChaGangPwd(String account) {
        ChaGang chaGang = chaGangRepository.findByAccount(account);
        if (chaGang != null) {
            return chaGang.getPwd();
        }
        return null;
    }

    // 10分钟刷新一次列表，包括监控帐号和查岗账号
    @Scheduled(cron = "0 */10 * * * ?")
    public void refresh() {
        // 更新监控列表
        List<Robot> allWatchRobotList = robotRepository.findByParentIdIsNull();
        // 是否有删除帐号
        List<String> needDeleteFromWatchRobotList = new ArrayList<>();
        for (RobotPriorityDto robotPriorityDto : watchRobotList) {
            boolean noRecord = allWatchRobotList.stream().noneMatch(robot -> robot.getPhone().equals(robotPriorityDto.getAccount()));
            if (noRecord) {
                needDeleteFromWatchRobotList.add(robotPriorityDto.getAccount());
            }
        }
        // 删除已经删除的监控帐号
        watchRobotList.removeIf(robotPriorityDto -> needDeleteFromWatchRobotList.contains(robotPriorityDto.getAccount()));
        // 需要新增的帐号
        List<Robot> needAddFromWatchRobotList = new ArrayList<>();
        for (Robot robot : allWatchRobotList) {
            boolean noRecord = watchRobotList.stream().noneMatch(robotPriorityDto -> robotPriorityDto.getAccount().equals(robot.getPhone()));
            if (noRecord) {
                needAddFromWatchRobotList.add(robot);
            }
        }
        if (needAddFromWatchRobotList.size() > 0) {
            for (Robot robot : needAddFromWatchRobotList) {
                watchRobotList.add(new RobotPriorityDto(robot.getPhone()));
            }
        }
        // 更新查岗列表
        List<ChaGang> gangRecords = chaGangRepository.findAll();
        // 需要删除的查岗账号
        List<String> needDeleteFromChaGangRobotList = new ArrayList<>();
        for (RobotPriorityDto robotPriorityDto : chaGangRobotList) {
            boolean noRecord = gangRecords.stream().noneMatch(robot -> robot.getAccount().equals(robotPriorityDto.getAccount()));
            if (noRecord) {
                needDeleteFromChaGangRobotList.add(robotPriorityDto.getAccount());
            }
        }
        chaGangRobotList.removeIf(robotPriorityDto -> needDeleteFromChaGangRobotList.contains(robotPriorityDto.getAccount()));
        // 需要新增的查岗帐号
        List<ChaGang> needAddFromChaGangRobotList = new ArrayList<>();
        for (ChaGang gangRecord : gangRecords) {
            boolean noRecord = chaGangRobotList.stream().noneMatch(robotPriorityDto -> robotPriorityDto.getAccount().equals(gangRecord.getAccount()));
            if (noRecord) {
                needAddFromChaGangRobotList.add(gangRecord);
            }
        }
        if (needAddFromChaGangRobotList.size() > 0) {
            for (ChaGang robot : needAddFromChaGangRobotList) {
                chaGangRobotList.add(new RobotPriorityDto(robot.getAccount()));
            }
        }
    }

    // 更新上一次心跳的时间，代表正常运行一次，需要降低优先级
    public void updateLastTime(String account) {
        watchRobotList.stream().filter(robotPriorityDto -> robotPriorityDto.getAccount().equals(account))
                .findFirst()
                .ifPresent(robotPriorityDto -> {
                    robotPriorityDto.setLastTime(LocalDateTime.now());
                    robotPriorityDto.setPriority(0);
                });
        chaGangRobotList.stream().filter(robotPriorityDto -> robotPriorityDto.getAccount().equals(account))
                .findFirst()
                .ifPresent(robotPriorityDto -> {
                    robotPriorityDto.setLastTime(LocalDateTime.now());
                    robotPriorityDto.setPriority(0);
                });
    }

    // 更新，优先级+1
    public void addPriority(String account) {
        watchRobotList.stream().filter(robotPriorityDto -> robotPriorityDto.getAccount().equals(account))
                .findFirst()
                .ifPresent(robotPriorityDto -> robotPriorityDto.setPriority(robotPriorityDto.getPriority() + 1));
        chaGangRobotList.stream().filter(robotPriorityDto -> robotPriorityDto.getAccount().equals(account))
                .findFirst()
                .ifPresent(robotPriorityDto -> robotPriorityDto.setPriority(robotPriorityDto.getPriority() + 1));
    }

    // 20秒排序一次列表，包括监控帐号和查岗账号
    @Scheduled(cron = "*/20 * * * * ?")
    public void sortList() {
        // 按照优先级降序排序
        watchRobotList.sort(Comparator.comparing(RobotPriorityDto::getPriority).reversed());
        chaGangRobotList.sort(Comparator.comparing(RobotPriorityDto::getPriority).reversed());
    }


    // 获取一个监控的帐号
    public RobotPriorityDto getOneWatchRobot() {
        if (watchRobotList.size() == 0) {
            return null;
        }
        return watchRobotList.get(0);
    }

    // 获取一个查岗的帐号
    public RobotPriorityDto getOneChaGangRobot() {
        if (chaGangRobotList.size() == 0) {
            return null;
        }
        return chaGangRobotList.get(0);
    }


    public List<RobotPriorityDto> getTop20FromWatchRobotList() {
        List<RobotPriorityDto> robotPriorityDtoList = new ArrayList<>();
        for (RobotPriorityDto robotPriorityDto : watchRobotList) {
            robotPriorityDtoList.add(robotPriorityDto);
            if (robotPriorityDtoList.size() > 20) {
                break;
            }
        }
        return robotPriorityDtoList;
    }


    public List<RobotPriorityDto> getTop20FromChaGangRobotList() {
        List<RobotPriorityDto> robotPriorityDtoList = new ArrayList<>();
        for (RobotPriorityDto robotPriorityDto : chaGangRobotList) {
            robotPriorityDtoList.add(robotPriorityDto);
            if (robotPriorityDtoList.size() > 20) {
                break;
            }
        }
        return robotPriorityDtoList;
    }
}
