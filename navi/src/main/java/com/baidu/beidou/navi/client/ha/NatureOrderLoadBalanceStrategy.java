package com.baidu.beidou.navi.client.ha;

import java.lang.reflect.Method;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.beidou.navi.client.NaviRpcClient;
import com.baidu.beidou.navi.client.attachment.Attachment;
import com.baidu.beidou.navi.exception.rpc.HARpcException;
import com.baidu.beidou.navi.exception.rpc.RpcException;
import com.baidu.beidou.navi.util.Preconditions;

/**
 * ClassName: NatureOrderLoadBalanceStrategy <br/>
 * Function: Round Robin轮训负载均衡策略调用
 * 
 * @author Zhang Xu
 */
public class NatureOrderLoadBalanceStrategy implements LoadBalanceStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(NatureOrderLoadBalanceStrategy.class);

    /**
     * 失败策略
     */
    private FailStrategy failStrategy;

    /**
     * Creates a new instance of NatureOrderLoadBalanceStrategy.
     */
    public NatureOrderLoadBalanceStrategy() {

    }

    /**
     * Creates a new instance of RRLoadBalanceStrategy.
     * 
     * @param failStrategy
     */
    public NatureOrderLoadBalanceStrategy(FailStrategy failStrategy) {
        this.failStrategy = failStrategy;
    }

    /**
     * @see com.baidu.beidou.navi.client.ha.LoadBalanceStrategy#transport(java.util.List, java.lang.reflect.Method,
     *      java.lang.Object[])
     */
    @Override
    public Object transport(List<NaviRpcClient> clientList, Method method, Object[] args)
            throws Throwable {
        return transport(clientList, method, args, null);
    }

    /**
     * @see com.baidu.beidou.navi.client.ha.LoadBalanceStrategy#transport(java.util.List, java.lang.reflect.Method,
     *      java.lang.Object[], com.baidu.beidou.navi.client.attachment.Attachment)
     */
    @Override
    public Object transport(List<NaviRpcClient> clientList, Method method, Object[] args,
            Attachment attachment) throws Throwable {
        Preconditions.checkState(failStrategy != null, "fail strategy is not configured");
        int clientSize = clientList.size();
        for (int currRetry = 0; currRetry < failStrategy.getMaxRetryTimes()
                && currRetry < clientSize;) {
            NaviRpcClient client = clientList.get(currRetry);
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Call on " + client.getInfo() + " starts...");
                }
                return client.transport(method, args, attachment);
            } catch (RpcException e) {
                LOG.error("Call on " + client.getInfo() + " failed due to " + e.getMessage(), e);
                if (failStrategy.isQuitImmediately(currRetry, clientSize)) {
                    throw e;
                }
                LOG.info("Fail over to next if available...");
                continue;
            } finally {
                currRetry++;
            }
        }
        throw new HARpcException("Failed to transport on all clients");
    }

}
