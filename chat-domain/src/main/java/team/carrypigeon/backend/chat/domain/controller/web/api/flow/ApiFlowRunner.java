package team.carrypigeon.backend.chat.domain.controller.web.api.flow;

import com.yomahub.liteflow.flow.LiteflowResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.chat.domain.flow.FlowTxExecutor;

/**
 * HTTP API LiteFlow 链路执行器。
 *
 * <h2>职责</h2>
 * <p>
 * 封装 LiteFlow 链路的事务执行，并将异常转换为标准化错误。
 *
 * <h2>链路契约</h2>
 * <ul>
 *   <li>输入：Controller 将请求写入 {@link CPFlowKeys#REQUEST}</li>
 *   <li>输出：Result 节点将响应写入 {@link CPFlowKeys#RESPONSE}</li>
 *   <li>错误：节点通过 {@link CPProblemException} 抛出标准化错误</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @RestController
 * public class ApiMessageController {
 *
 *     private final ApiFlowRunner flowRunner;
 *
 *     @GetMapping("/api/channels/{cid}/messages")
 *     public ResponseEntity<?> listMessages(@PathVariable String cid, ...) {
 *         CPFlowContext context = new CPFlowContext();
 *         context.set(CPFlowKeys.REQUEST, new MessageListRequest(cid, ...));
 *         context.set(CPFlowKeys.AUTH_UID, uid);
 *
 *         flowRunner.executeOrThrow("api_messages_list", context);
 *
 *         return ResponseEntity.ok(context.get(CPFlowKeys.RESPONSE));
 *     }
 * }
 * }</pre>
 *
 * @see CPFlowKeys
 * @see CPProblemException
 */
@Service
public class ApiFlowRunner {

    private static final Logger log = LoggerFactory.getLogger(ApiFlowRunner.class);

    private final FlowTxExecutor flowTxExecutor;

    public ApiFlowRunner(FlowTxExecutor flowTxExecutor) {
        this.flowTxExecutor = flowTxExecutor;
    }

    /**
     * 执行 LiteFlow 链路（带事务）。
     * <p>
     * 如果链路执行失败，将异常转换为 {@link CPProblemException} 并抛出。
     *
     * @param chain   链路名称（对应 XML 配置中的 chain name）
     * @param context 上下文
     * @return 执行后的上下文（用于链式调用）
     * @throws CPProblemException 执行失败时抛出
     */
    public CPFlowContext executeOrThrow(String chain, CPFlowContext context) {
        log.debug("[ApiFlowRunner] 开始执行链路: chain={}", chain);

        LiteflowResponse resp = flowTxExecutor.executeWithTx(chain, context);

        if (!resp.isSuccess()) {
            Exception cause = resp.getCause();
            log.warn("[ApiFlowRunner] 链路执行失败: chain={}, error={}",
                    chain, cause != null ? cause.getMessage() : "unknown");

            if (cause instanceof CPProblemException pex) {
                throw pex;
            }
            throw new CPProblemException(CPProblem.of(500, "internal_error", "internal error"));
        }

        log.debug("[ApiFlowRunner] 链路执行成功: chain={}", chain);
        return context;
    }
}
