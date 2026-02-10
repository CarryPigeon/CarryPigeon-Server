package team.carrypigeon.backend.test.flow;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.flow.FlowTxExecutor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FlowTxExecutorTests.Config.class)
class FlowTxExecutorTests {

    @Configuration
    @EnableTransactionManagement
    static class Config {
        @Bean
        PlatformTransactionManager platformTransactionManager() {
            return new SimplePlatformTransactionManager();
        }

        @Bean
        FlowExecutor flowExecutor() {
            return mock(FlowExecutor.class);
        }

        @Bean
        FlowTxExecutor flowTxExecutor(FlowExecutor flowExecutor) {
            return new FlowTxExecutor(flowExecutor);
        }
    }

    @Autowired
    private FlowTxExecutor flowTxExecutor;

    @Autowired
    private FlowExecutor flowExecutor;

    @Test
    void executeWithTx_success_shouldReturnResponse() {
        LiteflowResponse resp = mock(LiteflowResponse.class);
        when(resp.isSuccess()).thenReturn(true);
        when(flowExecutor.execute2Resp(eq("c"), isNull(), any(Object[].class))).thenReturn(resp);

        LiteflowResponse out = flowTxExecutor.executeWithTx("c", new CPFlowContext());
        assertSame(resp, out);
    }

    @Test
    void executeWithTx_failed_shouldNotThrow() {
        LiteflowResponse resp = mock(LiteflowResponse.class);
        when(resp.isSuccess()).thenReturn(false);
        when(resp.getMessage()).thenReturn("fail");
        when(flowExecutor.execute2Resp(eq("c"), isNull(), any(Object[].class))).thenReturn(resp);

        assertDoesNotThrow(() -> flowTxExecutor.executeWithTx("c", new CPFlowContext()));
    }

    private static final class SimplePlatformTransactionManager implements PlatformTransactionManager {
        /**
         * 创建一个新的事务状态对象。
         *
         * @param definition 事务定义（本测试实现未使用）
         * @return 新建的 { SimpleTransactionStatus} 实例
         */
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        /**
         * 记录提交调用。
         *
         * @param status 当前事务状态对象
         */
        @Override
        public void commit(TransactionStatus status) {
        }

        /**
         * 记录回滚调用。
         *
         * @param status 当前事务状态对象
         */
        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
