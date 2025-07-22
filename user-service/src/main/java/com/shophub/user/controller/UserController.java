package com.shophub.user.controller;

import com.shophub.user.entity.User;
import com.shophub.user.service.UserService;
import com.shophub.user.feign.ProductServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * 用户控制器
 * 
 * 提供用户管理的 REST API 接口
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ProductServiceClient productServiceClient;
    
    /**
     * 创建新用户
     * POST /api/users
     */
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody User user) {
        try {
            User savedUser = userService.createUser(user);
            // 不返回密码
            savedUser.setPassword(null);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("创建用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有用户
     * GET /api/users
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.findAllUsers();
        // 不返回密码
        users.forEach(user -> user.setPassword(null));
        return ResponseEntity.ok(users);
    }
    
    /**
     * 根据ID获取用户
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(null);
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 根据用户名获取用户
     * GET /api/users/username/{username}
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        Optional<User> userOpt = userService.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(null);
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 更新用户信息
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody User userDetails) {
        try {
            User updatedUser = userService.updateUser(id, userDetails);
            updatedUser.setPassword(null);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("更新用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除用户
     * DELETE /api/users/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok().body("用户删除成功");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("删除用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 用户认证
     * POST /api/users/authenticate
     */
    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        boolean isAuthenticated = userService.authenticateUser(
            loginRequest.getUsername(), 
            loginRequest.getPassword()
        );
        
        if (isAuthenticated) {
            return ResponseEntity.ok().body("认证成功");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户名或密码错误");
        }
    }
    
    /**
     * 获取活跃用户数量
     * GET /api/users/stats/active-count
     */
    @GetMapping("/stats/active-count")
    public ResponseEntity<Long> getActiveUserCount() {
        long count = userService.getActiveUserCount();
        return ResponseEntity.ok(count);
    }
    
    /**
     * 演示服务间通信 - 检查产品是否存在
     * GET /api/users/check-product/{productId}
     */
    @GetMapping("/check-product/{productId}")
    public ResponseEntity<?> checkProduct(@PathVariable Long productId) {
        try {
            String result = productServiceClient.checkProductExists(productId);
            return ResponseEntity.ok("用户服务调用产品服务成功: " + result);
        } catch (Exception e) {
            return ResponseEntity.ok("产品服务暂时不可用，这是正常现象（产品服务可能未实现此接口）: " + e.getMessage());
        }
    }
    
    /**
     * 健康检查
     * GET /api/users/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("User Service is running!");
    }
    
    // 登录请求DTO
    public static class LoginRequest {
        private String username;
        private String password;
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
} 