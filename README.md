##HRPC
>HRPC is a light-weight high performance RPC framework base on Netty and Zookeeper.

##Features
* Serialize by protostuff
* High performance, load balance and failover
* Service registration and subscription base on zookeeper
* Support asynchronous or synchronous invoking
* Keep-Alived connection, reconnect to server automatically
* Dynamic proxy by cglib
* Write less do more
* Spring support


##HRPC Structure
![](http://i.imgur.com/gnoKl5b.png)

##Service Registry
![](http://i.imgur.com/ckd00L8.png)

##Server Tutorial

#####1. Spring configuration
    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xmlns:context="http://www.springframework.org/schema/context"
           xmlns:util="http://www.springframework.org/schema/util"
           xsi:schemaLocation="http://www.springframework.org/schema/beans
           http://www.springframework.org/schema/beans/spring-beans.xsd
           http://www.springframework.org/schema/context
           http://www.springframework.org/schema/context/spring-context.xsd http://www.springframework.org/schema/util http://www.springframework.org/schema/util/spring-util.xsd">

    
        <!--扫描需求发布的服务所在的包-->
        <context:component-scan base-package="com.yingjun.rpc.service.impl"/>
        <context:property-placeholder location="classpath:system.properties"/>
    
        <!--服务端配置-->
        <bean id="rpcServer" class="com.yingjun.rpc.server.RPCServer">
            <constructor-arg name="zookeeper" value="${zookeeper.address}"/>
            <constructor-arg name="serverAddress" value="${server.address}"/>
        </bean>
    </beans>

#####2. Service interfacne
    public interface UserService {
        public User getUser(String phone);
        public User updateUser(User user);
    }

#####3. Provide rpc service
    @HRPCService(UserService.class)
    public class UserServiceImpl implements UserService {
    
        @Override
        public User getUser(String phone) {
            User user =new User(111,"yingjun",phone);
            return user;
        }
    
        @Override
        public User updateUser(User user) {
            user.setName("yingjun@update");
            return user;
        }
    }


##Client Tutorial
#####1. Spring configuration
    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xmlns:context="http://www.springframework.org/schema/context"
           xmlns:util="http://www.springframework.org/schema/util"
           xsi:schemaLocation="http://www.springframework.org/schema/beans
           http://www.springframework.org/schema/beans/spring-beans.xsd
           http://www.springframework.org/schema/context
           http://www.springframework.org/schema/context/spring-context.xsd http://www.springframework.org/schema/util http://www.springframework.org/schema/util/spring-util.xsd">

    
        <context:annotation-config/>
        <context:property-placeholder location="classpath:system.properties"/>
        <!--客户端配置-->
        <bean id="rpcClient" class="com.yingjun.rpc.client.RPCClient">
            <constructor-arg name="zookeeper" value="${zookeeper.address}"/>
            <!--订阅需要用到的接口-->
            <constructor-arg name="interfaces">
                <list>
                    <value>com.yingjun.rpc.service.OrderService</value>
                    <value>com.yingjun.rpc.service.UserService</value>
                    <value>com.yingjun.rpc.service.GoodsService</value>
                </list>
            </constructor-arg>
        </bean>
    
    </beans>

#####2. Synchronous invoking
    UserService userService = rpcClient.createProxy(UserService.class);
    User user1 = userService.getUser("188888888");
    logger.info("result:" + user1.toString());


#####3. Asynchronous invoking
    AsyncRPCProxy asyncProxy = rpcClient.createAsyncProxy(UserService.class);
    asyncProxy.call("getUser", new AsyncRPCCallback() {
         @Override
         public void success(Object result) {
             logger.info("result:" + result.toString());
         }
    
         @Override
         public void fail(Exception e) {
             logger.error("result:" + e.getMessage());
         }
     }, "188888888");
     
##Why choose protostuff ?
![](http://s5.51cto.com/wyfs02/M01/81/13/wKioL1dFyJfgnbJ1AABqzuFWzhw689.jpg-s_1893809398.jpg)