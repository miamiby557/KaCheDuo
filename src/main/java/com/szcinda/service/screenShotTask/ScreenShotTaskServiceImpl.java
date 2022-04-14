package com.szcinda.service.screenShotTask;

import com.szcinda.repository.*;
import com.szcinda.service.PageResult;
import com.szcinda.service.SnowFlakeFactory;
import com.szcinda.service.TypeStringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import sun.misc.BASE64Encoder;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import java.io.File;
import java.io.FileInputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Transactional
public class ScreenShotTaskServiceImpl implements ScreenShotTaskService {
    private final ScreenShotTaskRepository screenShotTaskRepository;
    private final HistoryScreenShotTaskRepository historyScreenShotTaskRepository;
    private final SnowFlakeFactory snowFlakeFactory;
    private final FengXianRepository fengXianRepository;
    private final RobotRepository robotRepository;
    private final RobotTaskRepository robotTaskRepository;

    @Value("${file.save.path}")
    private String savePath;

    public ScreenShotTaskServiceImpl(ScreenShotTaskRepository screenShotTaskRepository, HistoryScreenShotTaskRepository historyScreenShotTaskRepository,
                                     FengXianRepository fengXianRepository, RobotRepository robotRepository, RobotTaskRepository robotTaskRepository) {
        this.screenShotTaskRepository = screenShotTaskRepository;
        this.historyScreenShotTaskRepository = historyScreenShotTaskRepository;
        this.fengXianRepository = fengXianRepository;
        this.robotRepository = robotRepository;
        this.robotTaskRepository = robotTaskRepository;
        this.snowFlakeFactory = SnowFlakeFactory.getInstance();
    }


    @Override
    public void error(ScreenShotTaskErrorDto dto) {
        ScreenShotTask screenShotTask = screenShotTaskRepository.findOne(dto.getId());
        HistoryScreenShotTask historyScreenShotTask = new HistoryScreenShotTask();
        BeanUtils.copyProperties(screenShotTask, historyScreenShotTask);
        historyScreenShotTask.setId(snowFlakeFactory.nextId("HT"));
        historyScreenShotTask.setType(TypeStringUtils.screen_status2);
        historyScreenShotTask.setMessage(dto.getMessage());
        screenShotTaskRepository.delete(screenShotTask);
        historyScreenShotTaskRepository.save(historyScreenShotTask);
    }

    @Override
    public List<ScreenShotTask> queryRunning(ScreenShotTaskParams params) {
        List<Robot> robots = robotRepository.findByOwner(params.getOwner());
        Specification<ScreenShotTask> specification = ((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!StringUtils.isEmpty(params.getVehicleNo())) {
                Predicate vehicleNo = criteriaBuilder.equal(root.get("vehicleNo"), params.getVehicleNo());
                predicates.add(vehicleNo);
            }
            if (params.getCreateTimeStart() != null) {
                Predicate timeStart = criteriaBuilder.greaterThanOrEqualTo(root.get("createTime"), params.getCreateTimeStart().atStartOfDay());
                predicates.add(timeStart);
            }
            if (params.getCreateTimeEnd() != null) {
                Predicate timeEnd = criteriaBuilder.lessThan(root.get("createTime"), params.getCreateTimeEnd().plusDays(1).atStartOfDay());
                predicates.add(timeEnd);
            }
            List<String> phones = robots.stream().map(Robot::getPhone).collect(Collectors.toList());
            Expression<String> exp2 = root.get("owner");
            predicates.add(exp2.in(phones));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        Sort order = new Sort(Sort.Direction.DESC, "createTime");
        return screenShotTaskRepository.findAll(specification, order);
    }

    @Override
    public PageResult<HistoryScreenShotTaskDto> query(ScreenShotTaskParams params) {
        List<Robot> robots = robotRepository.findByOwner(params.getOwner());
        Specification<HistoryScreenShotTask> specification = ((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!StringUtils.isEmpty(params.getVehicleNo())) {
                Predicate vehicleNo = criteriaBuilder.equal(root.get("vehicleNo"), params.getVehicleNo());
                predicates.add(vehicleNo);
            }
            if (params.getCreateTimeStart() != null) {
                Predicate timeStart = criteriaBuilder.greaterThanOrEqualTo(root.get("createTime"), params.getCreateTimeStart().atStartOfDay());
                predicates.add(timeStart);
            }
            if (params.getCreateTimeEnd() != null) {
                Predicate timeEnd = criteriaBuilder.lessThan(root.get("createTime"), params.getCreateTimeEnd().plusDays(1).atStartOfDay());
                predicates.add(timeEnd);
            }
            List<String> phones = robots.stream().map(Robot::getPhone).collect(Collectors.toList());
            Expression<String> exp2 = root.get("owner");
            predicates.add(exp2.in(phones));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        Sort order = new Sort(Sort.Direction.DESC, "createTime");
        Pageable pageable = new PageRequest(params.getPage() - 1, params.getPageSize(), order);
        Page<HistoryScreenShotTask> details = historyScreenShotTaskRepository.findAll(specification, pageable);
        List<HistoryScreenShotTaskDto> dtos = new ArrayList<>();
        if (details.getContent() != null) {
            /*List<String> fxIds = details.getContent().stream().filter(item -> StringUtils.hasText(item.getFxId())).map(HistoryScreenShotTask::getFxId).collect(Collectors.toList());
            List<FengXian> fengXianList = fengXianRepository.findAll(fxIds);*/
            for (HistoryScreenShotTask historyScreenShotTask : details.getContent()) {
                HistoryScreenShotTaskDto taskDto = new HistoryScreenShotTaskDto();
                BeanUtils.copyProperties(historyScreenShotTask, taskDto);
                /*if (StringUtils.hasText(historyScreenShotTask.getFilePath())) {
                    File saveFile = new File(savePath, historyScreenShotTask.getFilePath());
                    if (saveFile.exists()) {
                        FileInputStream inputFile = null;
                        try {
                            inputFile = new FileInputStream(saveFile);
                            byte[] buffer = new byte[(int) saveFile.length()];
                            inputFile.read(buffer);
                            inputFile.close();
                            taskDto.setFileBase64(new BASE64Encoder().encode(buffer));
                        } catch (Exception ignored) {

                        } finally {
                            assert inputFile != null;
                            try {
                                inputFile.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                } else {
                    // 把图片转成base64
                    fengXianList.stream().filter(fx -> fx.getId().equals(historyScreenShotTask.getFxId()))
                            .findFirst()
                            .ifPresent(fx -> {
                                if (StringUtils.hasText(fx.getFilePath())) {
                                    File saveFile = new File(savePath, fx.getFilePath());
                                    if (!saveFile.exists()) {
                                        return;
                                    }
                                    FileInputStream inputFile = null;
                                    try {
                                        inputFile = new FileInputStream(saveFile);
                                        byte[] buffer = new byte[(int) saveFile.length()];
                                        inputFile.read(buffer);
                                        inputFile.close();
                                        taskDto.setFileBase64(new BASE64Encoder().encode(buffer));
                                    } catch (Exception ignored) {

                                    } finally {
                                        assert inputFile != null;
                                        try {
                                            inputFile.close();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            });
                }*/
                dtos.add(taskDto);
            }
        }
        return PageResult.of(dtos, params.getPage(), params.getPageSize(), details.getTotalElements());
    }

    @Override
    public ScreenShotTask findOneMission(String ownerWechat) {
        // 找一条已回复或者超时未回复的截图处理
        return screenShotTaskRepository.findFirstByOwnerWechatAndStatusIn(ownerWechat, Arrays.asList(TypeStringUtils.wechat_status2, TypeStringUtils.wechat_status4));
    }

    @Override
    public ScreenShotTask findOneSendMission(String ownerWechat) {
        // 找一条待发送的任务进行发送
        return screenShotTaskRepository.findFirstByOwnerWechatAndStatus(ownerWechat, TypeStringUtils.wechat_status3);
    }

    @Override
    public void finishSend(String screenShotId) {
        // 完成发送任务，设置处置的发送时间
        ScreenShotTask screenShotTask = screenShotTaskRepository.findOne(screenShotId);
        screenShotTask.setStatus(TypeStringUtils.wechat_status1);
        if (StringUtils.hasText(screenShotTask.getFxId())) {
            FengXian fengXian = fengXianRepository.findOne(screenShotTask.getFxId());
            if (fengXian != null) {
                fengXian.setMessageSendTime(LocalDateTime.now().toString().replace('T', ' '));
                fengXianRepository.save(fengXian);
            }
        }
        screenShotTaskRepository.save(screenShotTask);
    }

    @Override
    public void reRunTask(String id) {
        HistoryScreenShotTask historyScreenShotTask = historyScreenShotTaskRepository.findOne(id);
        ScreenShotTask screenShotTask = new ScreenShotTask();
        BeanUtils.copyProperties(historyScreenShotTask, screenShotTask, "id", "version");
        screenShotTask.setId(snowFlakeFactory.nextId("ST"));
        screenShotTask.setCreateTime(LocalDateTime.now());
        screenShotTask.setStatus(TypeStringUtils.wechat_status2);
        screenShotTaskRepository.save(screenShotTask);
        // 并且把正在运行的关于这个车牌的处理任务都删除
        List<RobotTask> robotTasks = robotTaskRepository.findByVehicleNoAndTaskTypeAndTaskStatus(historyScreenShotTask.getVehicleNo(), TypeStringUtils.robotType3, TypeStringUtils.taskStatus1);
        robotTaskRepository.delete(robotTasks);
    }

    // 1小时内如果没有收到回复，则把当前任务设为超时未回复，需要截图做处理
    @Scheduled(cron = "0 */2 * * * ?")
    public void checkIsNoRepeatInOneHour() {
        List<ScreenShotTask> noRepeatList = screenShotTaskRepository.findByStatus(TypeStringUtils.wechat_status1);
        if (noRepeatList != null) {
            LocalDateTime now = LocalDateTime.now();
            for (ScreenShotTask screenShotTask : noRepeatList) {
                Duration duration = Duration.between(now, screenShotTask.getCreateTime());
                long minutes = Math.abs(duration.toMinutes());//相差的分钟数
                if (minutes > 30) {
                    //需要截图并处理，不等司机回复
                    screenShotTask.setStatus(TypeStringUtils.wechat_status4);
                    screenShotTaskRepository.save(screenShotTask);
                }
            }
        }
    }

}
