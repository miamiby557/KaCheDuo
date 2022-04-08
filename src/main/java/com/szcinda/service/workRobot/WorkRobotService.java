package com.szcinda.service.workRobot;

import com.szcinda.repository.WorkRobot;
import com.szcinda.repository.WorkRobotRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class WorkRobotService {
    private final WorkRobotRepository workRobotRepository;

    public WorkRobotService(WorkRobotRepository workRobotRepository) {
        this.workRobotRepository = workRobotRepository;
    }

    // 删除超出时间范围内的工作机器人
    @Scheduled(cron = "0/30 * * * * ?")
    public void checkAlive() throws Exception {
        List<WorkRobot> workRobots = workRobotRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        List<WorkRobot> deleteDataList = new ArrayList<>();
        for (WorkRobot workRobot : workRobots) {
            LocalDateTime time = workRobot.getCreateTime();
            Duration duration = Duration.between(now, time);
            long minutes = Math.abs(duration.toMinutes());//相差的分钟数
            // 工作了15分钟没有完成任务，代表已经假死了
            if (minutes >= 15) {
                deleteDataList.add(workRobot);
            }
        }
        if (deleteDataList.size() > 0) {
            workRobotRepository.delete(deleteDataList);
        }
        deleteDataList = null;
    }
}
