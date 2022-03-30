package com.szcinda.service.robotTask;

import com.szcinda.repository.Robot;
import com.szcinda.repository.RobotRepository;
import com.szcinda.repository.RobotTask;
import com.szcinda.repository.RobotTaskRepository;
import com.szcinda.service.PageResult;
import com.szcinda.service.SnowFlakeFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Service
@Transactional

public class RobotTaskServiceImpl implements RobotTaskService {

    private final RobotTaskRepository robotTaskRepository;
    private final SnowFlakeFactory snowFlakeFactory;
    private final RobotRepository robotRepository;

    private static final ConcurrentLinkedQueue<String> canCreateTaskUserQueue = new ConcurrentLinkedQueue<>();

    public RobotTaskServiceImpl(RobotTaskRepository robotTaskRepository, RobotRepository robotRepository) {
        this.robotTaskRepository = robotTaskRepository;
        this.robotRepository = robotRepository;
        this.snowFlakeFactory = SnowFlakeFactory.getInstance();
    }

    @Override
    public PageResult<RobotTask> query(RobotTaskQuery params) {
        List<Robot> robots = robotRepository.findByOwner(params.getOwner());
        Specification<RobotTask> specification = ((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!StringUtils.isEmpty(params.getUserName())) {
                Predicate phone = criteriaBuilder.like(root.get("userName"), params.getUserName());
                predicates.add(phone);
            }
            if (!StringUtils.isEmpty(params.getTaskStatus())) {
                Predicate taskStatus = criteriaBuilder.equal(root.get("taskStatus"), params.getTaskStatus());
                predicates.add(taskStatus);
            }
            if (!StringUtils.isEmpty(params.getTaskType())) {
                Predicate taskType = criteriaBuilder.equal(root.get("taskType"), params.getTaskType());
                predicates.add(taskType);
            }
            if (params.getCreateTimeStart() != null) {
                Predicate timeStart = criteriaBuilder.greaterThanOrEqualTo(root.get("createTime"), params.getCreateTimeStart().atStartOfDay());
                predicates.add(timeStart);
            }
            if (params.getCreateTimeEnd() != null) {
                Predicate timeEnd = criteriaBuilder.lessThan(root.get("createTime"), params.getCreateTimeEnd().plusDays(1).atStartOfDay());
                predicates.add(timeEnd);
            }
            if (params.isQueryRunning()) {
                List<String> statusInList = new ArrayList<>();
                statusInList.add("待运行");
                statusInList.add("运行中");
                Expression<String> exp = root.get("taskStatus");
                predicates.add(exp.in(statusInList));
            } else {
                List<String> statusInList = new ArrayList<>();
                statusInList.add("已完成");
                statusInList.add("运行失败");
                Expression<String> exp = root.get("taskStatus");
                predicates.add(exp.in(statusInList));
            }
            List<String> phones = robots.stream().map(Robot::getPhone).collect(Collectors.toList());
            // 加上处理的帐号
            phones.addAll(robots.stream().filter(robot -> StringUtils.hasText(robot.getAccount2())).map(Robot::getAccount2).collect(Collectors.toList()));
            Expression<String> exp = root.get("userName");
            predicates.add(exp.in(phones));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        Sort order = new Sort(Sort.Direction.DESC, "createTime");
        Pageable pageable = new PageRequest(params.getPage() - 1, params.getPageSize(), order);
        Page<RobotTask> details = robotTaskRepository.findAll(specification, pageable);
        return PageResult.of(details.getContent(), params.getPage(), params.getPageSize(), details.getTotalElements());
    }


    @Override
    public void create(CreateRobotTaskDto dto) {
        List<String> statusList = new ArrayList<>();
        statusList.add("待运行");
        statusList.add("运行中");
        List<RobotTask> tasks = robotTaskRepository.findByUserNameAndTaskTypeAndTaskStatusIn(dto.getUserName(), dto.getTaskType(), statusList);
        if (tasks.size() == 0) {
            RobotTask task = new RobotTask();
            BeanUtils.copyProperties(dto, task);
            task.setId(snowFlakeFactory.nextId("RT"));
            task.setTaskStatus("待运行");
            robotTaskRepository.save(task);
        }
    }

    @Override
    public void run(String id) {
        RobotTask task = robotTaskRepository.findById(id);
        task.setTaskStatus("运行中");
        robotTaskRepository.save(task);
    }

    @Override
    public void error(TaskErrorDto errorDto) {
        RobotTask task = robotTaskRepository.findById(errorDto.getId());
        task.setTaskStatus("运行失败");
        task.setMessage(errorDto.getMessage());
        task.setFinishTime(LocalDateTime.now());
        robotTaskRepository.save(task);
    }

    @Override
    public void finish(String id) {
        RobotTask task = robotTaskRepository.findById(id);
        task.setTaskStatus("已完成");
        task.setFinishTime(LocalDateTime.now());
        robotTaskRepository.save(task);
    }

    @Override
    public List<RobotTask> getStandByList() {
        return robotTaskRepository.findByTaskStatus("待运行");
    }

    @Override
    public boolean checkIsStandby(String id) {
        RobotTask task = robotTaskRepository.findById(id);
        return "待运行".equals(task.getTaskStatus());
    }

    @Override
    public void createTask(String userName) {
        if (!canCreateTaskUserQueue.contains(userName)) {
            canCreateTaskUserQueue.add(userName);
        }

    }

    // 定时单线程处理是否有任务需要创建
    @Scheduled(cron = "0/10 * * * * ?")
    public void scheduleCreateTaskOneByOne() throws Exception {
        while (!canCreateTaskUserQueue.isEmpty()) {
            String userName = canCreateTaskUserQueue.poll();
            if (userName != null) {
                Robot robotUserName = robotRepository.findByPhone(userName);
                List<Robot> robots = robotRepository.findByParentId(robotUserName.getId());
                if (robots.size() > 0) {
                    CreateRobotTaskDto dto;
                    for (Robot robot : robots) {
                        if (robot.isRun()) {
                            // 所有的子账号一起配合工作
                            dto = new CreateRobotTaskDto();
                            dto.setUserName(robot.getPhone());
                            dto.setPwd(robot.getPwd());
                            dto.setCompany(robot.getCompany());
                            dto.setTaskType("处置");
                            this.create(dto);
                        }
                    }
                }
            }
        }
    }

    // 15分钟定时处理假死的任务
    @Scheduled(cron = "0 */2 * * * ?")
    public void dropDeadTaskOut15Minutes() throws Exception {
        List<RobotTask> tasks = robotTaskRepository.findByTaskStatus("运行中");
        if (tasks.size() > 0) {
            List<String> taskIds = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (RobotTask task : tasks) {
                LocalDateTime time = task.getCreateTime();
                Duration duration = Duration.between(now, time);
                long minutes = Math.abs(duration.toMinutes());//相差的分钟数
                if (minutes >= 15) {
                    taskIds.add(task.getId());
                }
            }
            if (taskIds.size() > 0) {
                for (String taskId : taskIds) {
                    robotTaskRepository.delete(taskId);
                }
            }
        }
    }

    @Override
    public void reRun(String id) {
        RobotTask task = robotTaskRepository.findById(id);
        List<String> stausList = new ArrayList<>();
        stausList.add("待运行");
        stausList.add("运行中");
        List<RobotTask> tasks = robotTaskRepository.findByUserNameAndTaskTypeAndTaskStatusIn(task.getUserName(), task.getTaskType(), stausList);
        if (tasks.size() == 0) {
            task.setTaskStatus("待运行");
            robotTaskRepository.save(task);
        } else {
            task.setTaskStatus("待运行");
            robotTaskRepository.save(task);
            // 其他相同任务丢弃
            for (RobotTask robotTask : tasks) {
                if (!task.getId().equals(robotTask.getId())) {
                    robotTaskRepository.delete(robotTask);
                }
            }
        }
    }

    @Override
    public void createAllUserNameTask() {
        List<Robot> robotRepositoryByParentIdIsNull = robotRepository.findByParentIdIsNull();
        for (Robot robot : robotRepositoryByParentIdIsNull) {
            canCreateTaskUserQueue.add(robot.getPhone());
        }
    }
}
