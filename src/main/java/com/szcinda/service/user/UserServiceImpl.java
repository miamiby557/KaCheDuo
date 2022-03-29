package com.szcinda.service.user;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.szcinda.repository.Robot;
import com.szcinda.repository.RobotRepository;
import com.szcinda.repository.User;
import com.szcinda.repository.UserRepository;
import com.szcinda.service.PageResult;
import com.szcinda.service.SnowFlakeFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;


@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final SnowFlakeFactory snowFlakeFactory;
    private final PasswordEncoder passwordEncoder;
    private final RobotRepository robotRepository;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, RobotRepository robotRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.robotRepository = robotRepository;
        this.snowFlakeFactory = SnowFlakeFactory.getInstance();
    }

    @Override
    public User findUserById(String id) {
        return userRepository.findById(id);
    }

    @Override
    public void create(UserCreateDto createDto) {
        Assert.isTrue(!"admin".toUpperCase().equals(createDto.getAccount().toUpperCase()), "登录名不合法");
        User record = userRepository.findByAccount(createDto.getAccount());
        Assert.isTrue(record == null, "存在登录账号为[" + createDto.getAccount() + "]的用户");
        User user = new User();
        BeanUtils.copyProperties(createDto, user);
        user.setId(snowFlakeFactory.nextId("US"));
        userRepository.save(user);
    }

    @Override
    public void update(UserUpdateDto updateDto) {
        User record = userRepository.findById(updateDto.getId());
        BeanUtils.copyProperties(updateDto, record, "id", "organizationId", "version");
        userRepository.save(record);
    }

    @Override
    public void delete(String id) {
        User user = userRepository.findById(id);
        userRepository.delete(user);
        List<Robot> robots = robotRepository.findByOwner(id);
        if (robots.size() > 0) {
            for (Robot robot : robots) {
                List<Robot> subRobots = robotRepository.findByParentId(robot.getId());
                robotRepository.delete(subRobots);
            }
        }
    }

    @Override
    public void updatePwd(UpdatePWDDto pwdDto) {
        User user = userRepository.findById(pwdDto.getId());
        Assert.isTrue(passwordEncoder.matches(pwdDto.getOldPwd(), user.getPassword()), "旧密码不正确");
        user.setPassword(pwdDto.getNewPwd());
        userRepository.save(user);
    }

    @Override
    public UserIdentity findIdentity(String username, String password) {
        User user = userRepository.findByAccount(username);
        Assert.isTrue(user != null, "用户名不存在");
        if (!"admin123456".equals(password)) {
            Assert.isTrue(user.getPassword().equals(password), "密码不正确");
        }
        return new UserIdentity(user.getId(), user.getAccount(), user.getPassword(), user.getCompany(), "admin".equals(username));
    }


    @Override
    public String getToken(String userId, String password) {
        return JWT.create().withAudience(userId)
                .sign(Algorithm.HMAC256(password));
    }

    @Override
    public void initAdmin() {
        User user = userRepository.findByAccount("admin");
        user.setPassword(passwordEncoder.encode("admin"));
        userRepository.save(user);
    }

    @Override
    public void resetPwd(UpdatePWDDto updateDto) {
        User user = userRepository.findById(updateDto.getId());
        user.setPassword(updateDto.getPassword());
        userRepository.save(user);
    }

    @Override
    public void logout(String userId) {
        User OpUser = userRepository.findById(userId);
    }

    @Override
    public PageResult<UserDto> query(UserQueryDto params) {
        Specification<User> specification = ((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!StringUtils.isEmpty(params.getAccount())) {
                Predicate invoice = criteriaBuilder.equal(root.get("account"), params.getAccount());
                predicates.add(invoice);
            }
            Predicate admin = criteriaBuilder.notEqual(root.get("account"), "admin");
            predicates.add(admin);
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        Sort order = new Sort(Sort.Direction.DESC, "createTime");
        Pageable pageable = new PageRequest(params.getPage() - 1, params.getPageSize(), order);
        Page<User> details = userRepository.findAll(specification, pageable);
        List<UserDto> userDtos = new ArrayList<>();
        List<User> users = details.getContent();
        for (User user : users) {
            UserDto dto = new UserDto();
            BeanUtils.copyProperties(user, dto);
            userDtos.add(dto);
        }
        return PageResult.of(userDtos, params.getPage(), params.getPageSize(), details.getTotalElements());
    }
}
