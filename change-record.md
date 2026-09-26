1. 修改账号密码注册api，需要携带邮箱以及验证码才能进行注册。
2. 邮箱验证码获取令牌不再自动创建新账户，相关响应里也不再存在相关newUser字段
3. 新增编码规范，new command需要另提一行
4. email加入到Account表中，user_profile表取消掉nickname字段
5. 将用户资料获取放入到profile模块中
