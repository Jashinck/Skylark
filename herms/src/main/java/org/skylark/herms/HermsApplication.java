package org.skylark.herms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Herms Workflow Engine — entry point.
 *
 * <p>赫姆斯（Hermes，信使之神）是 Skylark 生态中的工作流编排引擎，
 * 负责将多个 AI 处理节点串联成可复用的流水线，实现跨服务消息路由与
 * 条件分支执行。</p>
 */
@SpringBootApplication
public class HermsApplication {

    public static void main(String[] args) {
        SpringApplication.run(HermsApplication.class, args);
    }
}
