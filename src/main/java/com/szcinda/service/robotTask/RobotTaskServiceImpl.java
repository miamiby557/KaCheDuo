package com.szcinda.service.robotTask;

import com.szcinda.repository.*;
import com.szcinda.service.PageResult;
import com.szcinda.service.SnowFlakeFactory;
import com.szcinda.service.TypeStringUtils;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@Transactional

public class RobotTaskServiceImpl implements RobotTaskService {

    private final RobotTaskRepository robotTaskRepository;
    private final HistoryTaskRepository historyTaskRepository;
    private final SnowFlakeFactory snowFlakeFactory;
    private final RobotRepository robotRepository;
    private final FengXianRepository fengXianRepository;
    private final WorkRobotRepository workRobotRepository;


    private static final ReentrantLock lock = new ReentrantLock(true);

    // 正在处理的帐号
    public final static ConcurrentHashMap<String, LocalDateTime> handleAccountMap = new ConcurrentHashMap<>();

    private static final ConcurrentLinkedQueue<String> canCreateTaskUserQueue = new ConcurrentLinkedQueue<>();

    public RobotTaskServiceImpl(RobotTaskRepository robotTaskRepository, HistoryTaskRepository historyTaskRepository, RobotRepository robotRepository, FengXianRepository fengXianRepository, WorkRobotRepository workRobotRepository) {
        this.robotTaskRepository = robotTaskRepository;
        this.historyTaskRepository = historyTaskRepository;
        this.robotRepository = robotRepository;
        this.fengXianRepository = fengXianRepository;
        this.workRobotRepository = workRobotRepository;
        this.snowFlakeFactory = SnowFlakeFactory.getInstance();
    }

    @Override
    public PageResult<HistoryTask> query(RobotTaskQuery params) {
        List<Robot> robots = robotRepository.findByOwner(params.getOwner());
        Specification<HistoryTask> specification = ((root, criteriaQuery, criteriaBuilder) -> {
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
                statusInList.add(TypeStringUtils.taskStatus1);
                statusInList.add(TypeStringUtils.taskStatus2);
                Expression<String> exp = root.get("taskStatus");
                predicates.add(exp.in(statusInList));
            } else {
                List<String> statusInList = new ArrayList<>();
                statusInList.add(TypeStringUtils.taskStatus3);
                statusInList.add(TypeStringUtils.taskStatus4);
                Expression<String> exp = root.get("taskStatus");
                predicates.add(exp.in(statusInList));
            }
            List<String> phones = robots.stream().map(Robot::getPhone).collect(Collectors.toList());
            Expression<String> exp = root.get("userName");
            predicates.add(exp.in(phones));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        Sort order = new Sort(Sort.Direction.DESC, "createTime");
        Pageable pageable = new PageRequest(params.getPage() - 1, params.getPageSize(), order);
        Page<HistoryTask> details = historyTaskRepository.findAll(specification, pageable);
        return PageResult.of(details.getContent(), params.getPage(), params.getPageSize(), details.getTotalElements());
    }

    @Override
    public List<RobotTask> queryRunningTask(RobotTaskQuery params) {
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
            List<String> statusInList = new ArrayList<>();
            statusInList.add(TypeStringUtils.taskStatus1);
            statusInList.add(TypeStringUtils.taskStatus2);
            Expression<String> exp = root.get("taskStatus");
            predicates.add(exp.in(statusInList));
            List<String> phones = robots.stream().map(Robot::getPhone).collect(Collectors.toList());
            Expression<String> exp2 = root.get("userName");
            predicates.add(exp2.in(phones));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        Sort order = new Sort(Sort.Direction.DESC, "createTime");
        return robotTaskRepository.findAll(specification, order);
    }


    @Override
    public void create(CreateRobotTaskDto dto) {
        List<String> statusList = new ArrayList<>();
        statusList.add(TypeStringUtils.taskStatus1);
        statusList.add(TypeStringUtils.taskStatus2);
        List<RobotTask> tasks = robotTaskRepository.findByUserNameAndTaskTypeAndTaskStatusIn(dto.getUserName(), dto.getTaskType(), statusList);
        if (tasks.size() == 0) {
            RobotTask task = new RobotTask();
            BeanUtils.copyProperties(dto, task);
            task.setId(snowFlakeFactory.nextId("RT"));
            task.setTaskStatus(TypeStringUtils.taskStatus1);
            robotTaskRepository.save(task);
        }
    }

    @Override
    public void run(String id) {
        RobotTask task = robotTaskRepository.findById(id);
        task.setTaskStatus(TypeStringUtils.taskStatus2);
        robotTaskRepository.save(task);
    }

    @Override
    public void error(TaskErrorDto errorDto) {
        RobotTask task = robotTaskRepository.findById(errorDto.getId());
        task.setTaskStatus(TypeStringUtils.taskStatus4);
        task.setMessage(errorDto.getMessage());
        task.setFinishTime(LocalDateTime.now());
        HistoryTask historyTask = new HistoryTask();
        BeanUtils.copyProperties(task, historyTask);
        robotTaskRepository.delete(task);
        historyTaskRepository.save(historyTask);
        if (StringUtils.hasText(task.getFxId())) {
            // 更新处理
            FengXian fengXian = fengXianRepository.findOne(task.getFxId());
            fengXian.setChuLiTime(LocalDateTime.now());
            fengXian.setChuLiType(TypeStringUtils.fxHandleStatus3);
            fengXianRepository.save(fengXian);
        }
        // 从集合中删除正在运行的帐号
        workRobotRepository.deleteByUserName(task.getUserName());
    }

    @Override
    public void finish(String id) {
        RobotTask task = robotTaskRepository.findById(id);
        task.setTaskStatus(TypeStringUtils.taskStatus3);
        task.setFinishTime(LocalDateTime.now());
        HistoryTask historyTask = new HistoryTask();
        BeanUtils.copyProperties(task, historyTask);
        robotTaskRepository.delete(task);
        historyTaskRepository.save(historyTask);
        if (StringUtils.hasText(task.getFxId())) {
            // 更新处理
            FengXian fengXian = fengXianRepository.findOne(task.getFxId());
            fengXian.setChuLiTime(LocalDateTime.now());
            fengXian.setChuLiType(TypeStringUtils.fxHandleStatus2);
            fengXianRepository.save(fengXian);
        }
        // 从集合中删除正在运行的帐号
        workRobotRepository.deleteByUserName(task.getUserName());
    }


    // 把相同账号的处理任务放在一起，前端可以一起处理同一账号的任务，提高效率
    @Override
    public List<RobotTaskDto> getStandByList() {
        try {
            if (lock.tryLock(3, TimeUnit.SECONDS)) {
                List<RobotTaskDto> filterTasks = new ArrayList<>();
                // 获取正在工作的处理账号
                List<WorkRobot> workRobots = workRobotRepository.findAll();
                List<RobotTask> robotTasks = robotTaskRepository.findByTaskStatus(TypeStringUtils.taskStatus1);
                for (RobotTask robotTask : robotTasks) {
                    if (TypeStringUtils.robotType2.equals(robotTask.getTaskType())) {
                        RobotTaskDto dto = new RobotTaskDto();
                        BeanUtils.copyProperties(robotTask, dto);
                        filterTasks.add(dto);
                    } else if (TypeStringUtils.robotType3.equals(robotTask.getTaskType())) {
                        // 过滤不是正在运行中的帐号，避免帐号冲突
                        boolean noWork = workRobots.stream().noneMatch(item -> item.getUserName().equals(robotTask.getUserName()));
                        if (noWork) {
                            addToSubTasks(filterTasks, robotTask);
                        }
                    }
                }
                return filterTasks;
            }
        } catch (Exception ignored) {

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return null;
    }

    // 把相同账号的处理任务放在一起
    private void addToSubTasks(List<RobotTaskDto> filterTasks, RobotTask robotTask) {
        RobotTaskDto hasRecord = filterTasks.stream().filter(robotTaskDto -> robotTaskDto.getUserName().equals(robotTask.getUserName()))
                .findFirst()
                .orElse(null);
        if (hasRecord != null) {
            RobotTaskDto dto = new RobotTaskDto();
            BeanUtils.copyProperties(robotTask, dto);
            hasRecord.getSubTasks().add(dto);
        } else {
            RobotTaskDto dto = new RobotTaskDto();
            BeanUtils.copyProperties(robotTask, dto);
            filterTasks.add(dto);
        }
    }

    @Override
    public boolean checkIsStandby(String id) {
        RobotTask task = robotTaskRepository.findById(id);
        return TypeStringUtils.taskStatus1.equals(task.getTaskStatus());
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
                        if (TypeStringUtils.robotType2.equals(robot.getType()) && robot.isRun()) {
                            // 所有的子账号一起配合工作
                            dto = new CreateRobotTaskDto();
                            dto.setUserName(robot.getPhone());
                            dto.setPwd(robot.getPwd());
                            dto.setCompany(robot.getCompany());
                            dto.setTaskType(TypeStringUtils.robotType2);
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
        List<RobotTask> tasks = robotTaskRepository.findByTaskStatus(TypeStringUtils.taskStatus2);
        if (tasks.size() > 0) {
            List<String> taskIds = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (RobotTask task : tasks) {
                LocalDateTime time = task.getCreateTime();
                Duration duration = Duration.between(now, time);
                long minutes = Math.abs(duration.toMinutes());//相差的分钟数
                long minNumber = 15;
                if (TypeStringUtils.robotType3.equals(task.getTaskType())) {
                    // 如果是位置监控，需要的时间长
                    minNumber = 60;
                }
                if (minutes >= minNumber) {
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
        stausList.add(TypeStringUtils.taskStatus1);
        stausList.add(TypeStringUtils.taskStatus2);
        List<RobotTask> tasks = robotTaskRepository.findByUserNameAndTaskTypeAndTaskStatusIn(task.getUserName(), task.getTaskType(), stausList);
        if (tasks.size() == 0) {
            task.setTaskStatus(TypeStringUtils.taskStatus1);
            robotTaskRepository.save(task);
        } else {
            task.setTaskStatus(TypeStringUtils.taskStatus1);
            robotTaskRepository.save(task);
            workRobotRepository.deleteByUserName(task.getUserName());
            // 只有处置类型的可以删除
            if (TypeStringUtils.robotType2.equals(task.getTaskType())) {
                // 其他相同任务丢弃
                for (RobotTask robotTask : tasks) {
                    if (!task.getId().equals(robotTask.getId()) && TypeStringUtils.robotType2.equals(task.getTaskType())) {
                        robotTaskRepository.delete(robotTask);
                    }
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

    // 完成后剔除帐号
    @Override
    public void release(String userName) {
        workRobotRepository.deleteByUserName(userName);
    }

    @Override
    public void lock(String userName) {
        WorkRobot workRobot = workRobotRepository.findByUserName(userName);
        if (workRobot == null) {
            workRobot = new WorkRobot();
            workRobot.setId(snowFlakeFactory.nextId("WB"));
            workRobot.setUserName(userName);
            workRobotRepository.save(workRobot);
        }
    }
}
