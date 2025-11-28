# AGENTS Guidelines
## 0. 阅读须知

- 本指南适用于`chat-domain`模块，该模块内部实现了聊天服务的具体逻辑。
- 坚持“强制优先、结果导向、可审计”，所有流程需可追溯。
- 若与本指南冲突的用户显式指令出现，必须遵循并在前置说明记录偏差原因。

## 1. 项目目录

项目源码的根目录为`src/main/java`，其内部包含了该模块的源码，以下规定了对各级目录的介绍

项目包的根包为`team.carrypigeon.backend.chat.domain`

- `attribute` - 包含了`CPSession`的上下文数据的key值，例如`ChatDomainUserId`对应为当前用户的id的访问key值
- `cmp` - 包含了`liteflow`可调用的不同的组件
  - `basic` - 包含基础组件，用于对整体流程进行微调或者链接不同的流程，例如`[RenameArgNode.java](src/main/java/team/carrypigeon/backend/chat/domain/cmp/basic/RenameArgNode.java)`用于重命名liteflow上下文中的参数
  - `checker` - 包含校验组件，用于校验输入参数或者权限，不包含业务逻辑，值用于检查请求是否合法，例如检查邮箱是否符合规范、检查用户是否登录等功能
  - `notifier` - 包含通知组件，用于通知所有受当前请求影响的在线的用户，其根目录应该只存在一个`Notifier.java`文件，该文件用于通知所有受当前请求影响的在线用户，例如`[Notifier.java](src/main/java/team/carrypigeon/backend/chat/domain/cmp/notifier/Notifier.java)`用于通知上下文定义的用户，其子目录内部只应该定义Collector用于获取需要通知的用户，例如获取某个群聊的所有用户
  - `biz` - 包含业务组件，用于实现业务逻辑，不同的组件有不同的对上下文数据的操作行为，不同行为组合为一个具体的请求
  - `service` - 包含服务组件，用于调用`external-service`模块的具体的服务
  - `info` - 不包含`liteflow`组件，只用于对某些通用数据结构进行封装，例如分页查询的`PageInfo`
- `controller` - 存放请求处理类
  - `netty` - 存放netty请求处理类，用于处理长连接的的请求
  - `web` - 存放web请求处理类，用于处理web请求
- `config` - 配置相关文件夹，目前暂不需要具体修改
- `permission` - 已废弃，是需要重构删除的文件夹，重构完成后由本人进行删除
- `service` - 项目内容服务，包含通知服务、消息服务、文件服务，暂时不要进行修改


## 2. 请求开发规范

一个具体的请求由以下几个部分组成

1. 项目中的类用于springboot扫描，其应该使用@CPControllerTag进行标记，
   其的三个参数分别为请求链接`route`,请求实体类`voClazz`，响应实体类`resultClazz`
2. `liteflow`配置文件，其chain的name应该为对应controller类的`route`参数

### 2.1 route及其请求参数与响应参数规范

请你分析源代码中通过实现接口与抽象类的实现形式代码获取请求的具体信息，然后重构为最新的`liteflow`实现范式

### 2.2 voClazz规范

一个voClazz对应的实体类应该包含请求的具体参数与实现CPControllerVO接口，以下是一个例子

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPUserEmailLoginVO implements CPControllerVO {
    // 用户邮箱
    private String email;
    // 验证码
    private int code;

    @Override
    public boolean insertData(DefaultContext context) {
        context.setData("Email", email);
        context.setData("Email_Code", code);
        context.setData("UserInfo_Email", email);
        return true;
    }
}
```

其中insertData方法为将请求参数翻译转换到liteflow处理的上下文中，使其能被liteflow的不同组件利用

### 2.3 resultClazz规范

一个resultClazz对应类的标准实现形式如下：

```java
public class CPUserEmailLoginResult implements CPControllerResult {

    @Override
    public void process(CPSession session, DefaultContext context, ObjectMapper objectMapper) {
        CPUserToken userToken = context.getData("UserToken");
        if (userToken == null){
            argsError(context);
            return;
        }
        context.setData("response", CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(new Result(userToken.getToken()))));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    private static class Result{
        private String token;
    }
}
```

其应该实现CPControllerResult接口，其会有一个process方法，result应该在context中根据上下文中的数据组成一个响应值
以response为上下文的key值，以CPResponse的成功响应的示例的副本并传入特定的data为value值

如果该请求的响应值为默认响应值，则可直接定义为`resultClazz = CPControllerDefaultResult.class`

### liteflow配置规范

一个请求应该对应liteflow中的一个责任链，请根据不同的请求划分将其放入不同的`*.xml`文件

一个标准的用户配置为：

```xml
    <!--更新频道数据-->
    <chain name="/core/channel/profile/update">
        THEN(
        /**判断用户是否登录**/
        UserLoginChecker,
        /**重命名参数**/
        RenameArg.bind("RenameArg","SessionId:UserInfo_Id"),
        /**获取频道信息**/
        CPChannelSelector.bind("key","id"),
        /**权限校验**/
        CPChannelOwnerChecker,
        /**更新频道信息**/
        CPChannelUpdater,
        /**保存**/
        CPChannelSaver,
        /**获取成员信息**/
        CPChannelMemberCollector,
        /**通知所有人**/
        CPNotifier.bind("route","/core/channel/list")
        )
    </chain>
```

请你确保liteflow的上下文参数传递合理，即不会存在一个到达一个组件时不存在该组件需要的上下文参数

## 3. 组件开发规范

一般性的组件需要满足特定的规范，特殊组件请你根据业务需求酌情考虑是否增加但是必须获取我的同意才可以增加

### 3.0 组件定义规范

一个组件应该集成项目中的`[CPNodeComponent.java](../api/src/main/java/team/carrypigeon/backend/api/chat/domain/controller/CPNodeComponent.java)

`CPNodeComponent`继承自`NodeComponent`，其定义了组件的必要属性与方法

组件的注释应该包含以下内容：
1. 组件功能介绍
2. 如果有，需要包含组件bind参数的格式与含义
3. 组件所需要的上下文参数，即入参(name:type)形式，其可以通过bind参数的不同有不同的入参要求
4. 组件输出参数，即出参(name:type)形式,其在组件调用完后存在于上下文中

组件的参数校验：
组件的参数校验应该在组件的`process`方法中进行，应该在组件刚开始执行时就进行入参的存在性校验，如果某一个参数不存在则调用`argsError()`方法并返回

组件的中断：
某一个组件调用时如果发生了异常，则在context中加入相关的错误的response，然后抛出`CPReturnException`
### 3.1 基本组件

基本组件用于提供上下文中的基础性功能，例如对上下文中的参数进行删除与重命名操作，使得同一数据能够在不同的组件间使用

基本组件不应该依赖与任何组件，即不应该拥有入参与出参，只通过`bind("key","{value}")`进行配置

### 3.2 业务组件

业务组件根据模块划分放到不同的目录分层中，请你参考现有的目录分层进行分层的理解

业务的一般组件包含以下几种：

1. `xxxSelectorNode` - 数据库查询组件，用于从数据库中查询一个实体对象出来，其可酌情包含多种查询模式，例如[CPChannelMemberSelectorNode.java](src/main/java/team/carrypigeon/backend/chat/domain/cmp/biz/channel/member/CPChannelMemberSelectorNode.java)
2. `xxxUpdatorNode` - 根据上下的数据更新上下文中指定对象的数据，例如[CPChannelUpdaterNode.java](src/main/java/team/carrypigeon/backend/chat/domain/cmp/biz/channel/CPChannelUpdaterNode.java)
3. `xxxCreatorNode` - 根据上下文的特殊数据构建指定的对象的数据，例如[CPUUserTokenCreatorNode.java](src/main/java/team/carrypigeon/backend/chat/domain/cmp/biz/user/token/CPUUserTokenCreatorNode.java)
4. `xxxBuidlerNode` - 根据上下文的原始数据构建指定的对象的数据，例如[CPUserBuilderNode.java](src/main/java/team/carrypigeon/backend/chat/domain/cmp/biz/user/CPUserBuilderNode.java)
5. `xxxSavorNode` - 根据上下文的数据保存指定的对象数据，例如[CPUserSavorNode.java](src/main/java/team/carrypigeon/backend/chat/domain/cmp/biz/user/CPUserSavorNode.java)
6. `xxx{FieldName}GetterNode` - 根据上下文的数据获取`{FieldName}`字段的信息，例如[CPChannelApplicationCidGetterNode.java](src/main/java/team/carrypigeon/backend/chat/domain/cmp/biz/channel/application/CPChannelApplicationCidGetterNode.java)
7. `xxx{FieldName}SetterNode` - 根据上下文数据设置`{FieldName}`字段的信息，例如[CPChannelMemberAuthoritySetterNode.java](src/main/java/team/carrypigeon/backend/chat/domain/cmp/biz/channel/member/CPChannelMemberAuthoritySetterNode.java)
8. `xxxListerNode` - 根据上下文数据获取指定字段的列表信息，例如[CPChannelMemberListerNode.java](src/main/java/team/carrypigeon/backend/chat/domain/cmp/biz/channel/member/CPChannelMemberListerNode.java)
9. `xxxDeleterNode` - 根据上下文数据删除指定的字段，例如[CPChannelDeleterNode.java](src/main/java/team/carrypigeon/backend/chat/domain/cmp/biz/channel/CPChannelDeleterNode.java)

### 3.3 校验器组件

校验器组件的结构较为灵活，可根据参数校验的具体需求进行自定义

例如检验邮箱是否合法，检验用户是否在指定的群聊中，用户是否已经登录

### 3.4 通知组件

通知组件的具体通知操作已经实现

你只需要根据需求酌情增加相关的collector,例如[CPUserRelatedCollectorNode.java](src/main/java/team/carrypigeon/backend/chat/domain/cmp/notifier/user/CPUserRelatedCollectorNode.java)
