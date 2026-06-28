package com.dataspec.starterkit.service;

import com.dataspec.starterkit.model.StarterKitDefinition;
import com.dataspec.starterkit.model.StarterKitDomain;
import com.dataspec.starterkit.model.StarterKitEnumDefinition;
import com.dataspec.starterkit.model.StarterKitEnumValue;
import com.dataspec.starterkit.model.StarterKitFieldDefinition;
import com.dataspec.starterkit.model.StarterKitTemplateDefinition;
import com.dataspec.starterkit.model.StarterKitTemplateField;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 第一版内置领域 Starter Kit。
 *
 * <p>这些 kit 是给个人/小团队和 AI 快速起步使用的窄模板，不代表行业全量模型。</p>
 */
public final class BuiltInDomainStarterKits {

    public static final String VERSION = "2026.06";

    private static final List<StarterKitDefinition> KITS = List.of(
            userAccountKit(),
            orderTradeKit(),
            paymentAmountKit(),
            inventoryCatalogKit(),
            auditLogKit()
    ).stream().sorted(Comparator.comparing(StarterKitDefinition::key)).toList();

    private BuiltInDomainStarterKits() {
    }

    public static List<StarterKitDefinition> list() {
        return KITS;
    }

    public static Optional<StarterKitDefinition> find(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return KITS.stream()
                .filter(kit -> kit.key().equalsIgnoreCase(key.trim()))
                .findFirst();
    }

    private static StarterKitDefinition userAccountKit() {
        return new StarterKitDefinition(
                "user_account",
                "用户账号 Starter Kit",
                VERSION,
                "用户、会员、账号体系常用字段和状态枚举。",
                List.of("user", "account", "identity"),
                List.of("会员表", "账号表", "登录审计前置字段"),
                List.of(domain("user", "用户域", "用户、会员、账号相关标准")),
                List.of(enumDef("account_status", "账号状态", "账号生命周期状态", "string",
                        enumValue("active", "正常", 1),
                        enumValue("disabled", "停用", 2),
                        enumValue("locked", "锁定", 3))),
                List.of(
                        field("user_id", "用户ID", "bigint", null, null, null, false, null,
                                "用户主键 ID", "user", "identifier,user", "uid,member_id,account_id",
                                "identifier", null, false, "enabled", "10001"),
                        field("username", "用户名", "varchar", 64, null, null, false, null,
                                "用户登录名或展示名", "user", "user,profile", "login_name,nickname",
                                "identity", null, false, "enabled", "alice"),
                        field("mobile_phone", "手机号", "varchar", 32, null, null, true, null,
                                "用户手机号,导出给 AI 时需注意敏感性", "user", "contact,user", "phone,mobile,tel",
                                "contact", null, true, "enabled", "13800000000"),
                        field("email", "邮箱", "varchar", 128, null, null, true, null,
                                "用户邮箱地址", "user", "contact,user", "mail,email_address",
                                "contact", null, true, "enabled", "user@example.com"),
                        field("account_status", "账号状态", "varchar", 20, null, null, false, "'active'",
                                "账号状态,见 account_status 枚举", "user", "status,user", "user_status",
                                "status", "account_status", false, "enabled", "active"),
                        field("registered_at", "注册时间", "timestamp with time zone", null, null, null, false, null,
                                "用户注册时间", "user", "time,user", "signup_at,created_time",
                                "time", null, false, "enabled", "2026-01-01T10:00:00Z"),
                        field("last_login_at", "最近登录时间", "timestamp with time zone", null, null, null, true, null,
                                "用户最近一次登录时间", "user", "time,user", "login_at",
                                "time", null, false, "enabled", "2026-01-02T09:30:00Z")
                ),
                List.of(template("用户账号表模板", "适用于用户/会员账号表", "user",
                        tf("user_id", 1, true),
                        tf("username", 2, true),
                        tf("mobile_phone", 3, false),
                        tf("email", 4, false),
                        tf("account_status", 5, true),
                        tf("registered_at", 6, true),
                        customTf("created_at", "timestamp with time zone", false, null, "创建时间", 7, true),
                        customTf("updated_at", "timestamp with time zone", false, null, "更新时间", 8, true)))
        );
    }

    private static StarterKitDefinition orderTradeKit() {
        return new StarterKitDefinition(
                "order_trade",
                "订单交易 Starter Kit",
                VERSION,
                "订单主表常用字段、金额字段和订单状态枚举。",
                List.of("order", "trade", "money"),
                List.of("订单表", "交易表", "履约状态表"),
                List.of(domain("order", "订单域", "订单、交易和履约相关标准")),
                List.of(enumDef("order_status", "订单状态", "订单主流程状态", "string",
                        enumValue("pending", "待支付", 1),
                        enumValue("paid", "已支付", 2),
                        enumValue("shipped", "已发货", 3),
                        enumValue("completed", "已完成", 4),
                        enumValue("cancelled", "已取消", 5))),
                List.of(
                        field("order_id", "订单ID", "bigint", null, null, null, false, null,
                                "订单主键 ID", "order", "order,identifier", "trade_id",
                                "identifier", null, false, "enabled", "90001"),
                        field("order_no", "订单号", "varchar", 64, null, null, false, null,
                                "业务可见订单编号", "order", "order,identifier", "trade_no,order_code",
                                "identifier", null, false, "enabled", "O202606280001"),
                        field("user_id", "用户ID", "bigint", null, null, null, false, null,
                                "下单用户 ID", "order", "user,order,foreign_key", "member_id,uid",
                                "identifier", null, false, "enabled", "10001"),
                        field("total_amount_cent", "订单总金额(分)", "bigint", null, null, null, false, "0",
                                "订单总金额,单位为分", "order", "money,order", "total_fee,pay_amount_cent",
                                "money", null, false, "enabled", "129900"),
                        field("currency", "币种", "varchar", 3, null, null, false, "'CNY'",
                                "ISO 4217 币种代码", "order", "money,order", "currency_code",
                                "money", null, false, "enabled", "CNY"),
                        field("order_status", "订单状态", "varchar", 20, null, null, false, "'pending'",
                                "订单状态,见 order_status 枚举", "order", "status,order", "status",
                                "status", "order_status", false, "enabled", "pending"),
                        field("paid_at", "支付时间", "timestamp with time zone", null, null, null, true, null,
                                "订单支付成功时间", "order", "time,payment,order", "pay_time",
                                "time", null, false, "enabled", "2026-01-02T12:00:00Z")
                ),
                List.of(template("订单主表模板", "适用于订单/交易主表", "order",
                        tf("order_id", 1, true),
                        tf("order_no", 2, true),
                        tf("user_id", 3, true),
                        tf("total_amount_cent", 4, true),
                        tf("currency", 5, true),
                        tf("order_status", 6, true),
                        tf("paid_at", 7, false),
                        customTf("created_at", "timestamp with time zone", false, null, "创建时间", 8, true),
                        customTf("updated_at", "timestamp with time zone", false, null, "更新时间", 9, true)))
        );
    }

    private static StarterKitDefinition paymentAmountKit() {
        return new StarterKitDefinition(
                "payment_amount",
                "支付金额 Starter Kit",
                VERSION,
                "支付流水、支付渠道、退款金额等常用标准。",
                List.of("payment", "money", "channel"),
                List.of("支付流水表", "退款表", "渠道对账表"),
                List.of(domain("payment", "支付域", "支付、退款和资金相关标准")),
                List.of(
                        enumDef("payment_status", "支付状态", "支付流水状态", "string",
                                enumValue("pending", "待支付", 1),
                                enumValue("success", "支付成功", 2),
                                enumValue("failed", "支付失败", 3),
                                enumValue("refunded", "已退款", 4)),
                        enumDef("payment_channel", "支付渠道", "支付渠道编码", "string",
                                enumValue("alipay", "支付宝", 1),
                                enumValue("wechat", "微信支付", 2),
                                enumValue("card", "银行卡", 3))
                ),
                List.of(
                        field("payment_id", "支付ID", "bigint", null, null, null, false, null,
                                "支付流水主键 ID", "payment", "payment,identifier", "pay_id",
                                "identifier", null, false, "enabled", "70001"),
                        field("payment_no", "支付流水号", "varchar", 64, null, null, false, null,
                                "第三方或本系统支付流水号", "payment", "payment,identifier", "pay_no,transaction_no",
                                "identifier", null, false, "enabled", "P202606280001"),
                        field("order_id", "订单ID", "bigint", null, null, null, false, null,
                                "关联订单 ID", "payment", "payment,order,foreign_key", "trade_id",
                                "identifier", null, false, "enabled", "90001"),
                        field("payment_amount_cent", "支付金额(分)", "bigint", null, null, null, false, "0",
                                "支付金额,单位为分", "payment", "money,payment", "pay_amount_cent",
                                "money", null, false, "enabled", "129900"),
                        field("refund_amount_cent", "退款金额(分)", "bigint", null, null, null, false, "0",
                                "累计退款金额,单位为分", "payment", "money,refund", "refunded_fee",
                                "money", null, false, "enabled", "0"),
                        field("payment_status", "支付状态", "varchar", 20, null, null, false, "'pending'",
                                "支付状态,见 payment_status 枚举", "payment", "status,payment", "pay_status",
                                "status", "payment_status", false, "enabled", "success"),
                        field("payment_channel", "支付渠道", "varchar", 20, null, null, false, null,
                                "支付渠道,见 payment_channel 枚举", "payment", "payment,channel", "pay_channel",
                                "channel", "payment_channel", false, "enabled", "wechat")
                ),
                List.of(template("支付流水表模板", "适用于支付/退款流水表", "payment",
                        tf("payment_id", 1, true),
                        tf("payment_no", 2, true),
                        tf("order_id", 3, true),
                        tf("payment_amount_cent", 4, true),
                        tf("refund_amount_cent", 5, true),
                        tf("payment_status", 6, true),
                        tf("payment_channel", 7, true),
                        customTf("created_at", "timestamp with time zone", false, null, "创建时间", 8, true)))
        );
    }

    private static StarterKitDefinition inventoryCatalogKit() {
        return new StarterKitDefinition(
                "inventory_catalog",
                "库存商品 Starter Kit",
                VERSION,
                "商品、SKU、库存数量和仓库编码常用字段。",
                List.of("inventory", "product", "sku"),
                List.of("商品表", "库存表", "SKU 表"),
                List.of(domain("inventory", "库存域", "商品、SKU、库存和仓储相关标准")),
                List.of(enumDef("inventory_status", "库存状态", "库存记录状态", "string",
                        enumValue("available", "可售", 1),
                        enumValue("locked", "锁定", 2),
                        enumValue("out_of_stock", "缺货", 3))),
                List.of(
                        field("product_id", "商品ID", "bigint", null, null, null, false, null,
                                "商品主键 ID", "inventory", "product,identifier", "spu_id,item_id",
                                "identifier", null, false, "enabled", "30001"),
                        field("sku_code", "SKU编码", "varchar", 64, null, null, false, null,
                                "库存最小单元编码", "inventory", "sku,identifier", "sku_id",
                                "identifier", null, false, "enabled", "SKU-001"),
                        field("product_name", "商品名称", "varchar", 200, null, null, false, null,
                                "商品展示名称", "inventory", "product", "item_name",
                                "content", null, false, "enabled", "标准会员套餐"),
                        field("inventory_qty", "库存数量", "integer", null, null, null, false, "0",
                                "可售库存数量", "inventory", "inventory,quantity", "stock_qty,available_qty",
                                "quantity", null, false, "enabled", "100"),
                        field("locked_qty", "锁定库存数量", "integer", null, null, null, false, "0",
                                "下单未完成等场景锁定的库存数量", "inventory", "inventory,quantity", "freeze_qty",
                                "quantity", null, false, "enabled", "3"),
                        field("inventory_status", "库存状态", "varchar", 20, null, null, false, "'available'",
                                "库存状态,见 inventory_status 枚举", "inventory", "status,inventory", "stock_status",
                                "status", "inventory_status", false, "enabled", "available"),
                        field("warehouse_code", "仓库编码", "varchar", 64, null, null, true, null,
                                "仓库或门店编码", "inventory", "warehouse,inventory", "store_code",
                                "identifier", null, false, "enabled", "WH-SH-01")
                ),
                List.of(template("库存商品表模板", "适用于商品/SKU/库存表", "inventory",
                        tf("product_id", 1, true),
                        tf("sku_code", 2, true),
                        tf("product_name", 3, true),
                        tf("inventory_qty", 4, true),
                        tf("locked_qty", 5, true),
                        tf("inventory_status", 6, true),
                        tf("warehouse_code", 7, false),
                        customTf("updated_at", "timestamp with time zone", false, null, "更新时间", 8, true)))
        );
    }

    private static StarterKitDefinition auditLogKit() {
        return new StarterKitDefinition(
                "audit_log",
                "审计日志 Starter Kit",
                VERSION,
                "创建/更新人、时间戳、追踪 ID 和请求 ID 等横切审计字段。",
                List.of("audit", "trace", "time"),
                List.of("审计日志表", "业务表审计列", "请求追踪字段"),
                List.of(domain("audit", "审计域", "审计、追踪和软删除相关标准")),
                List.of(),
                List.of(
                        field("created_at", "创建时间", "timestamp with time zone", null, null, null, false, null,
                                "记录创建时间", "audit", "audit,time", "create_time",
                                "time", null, false, "enabled", "2026-01-01T10:00:00Z"),
                        field("updated_at", "更新时间", "timestamp with time zone", null, null, null, false, null,
                                "记录最后更新时间", "audit", "audit,time", "update_time,modified_at",
                                "time", null, false, "enabled", "2026-01-02T10:00:00Z"),
                        field("created_by", "创建人", "varchar", 100, null, null, true, null,
                                "创建记录的操作者", "audit", "audit,operator", "creator",
                                "operator", null, false, "enabled", "system"),
                        field("updated_by", "更新人", "varchar", 100, null, null, true, null,
                                "最后更新记录的操作者", "audit", "audit,operator", "modifier",
                                "operator", null, false, "enabled", "system"),
                        field("deleted_at", "删除时间", "timestamp with time zone", null, null, null, true, null,
                                "软删除时间", "audit", "audit,time,delete", "delete_time",
                                "time", null, false, "enabled", "2026-01-03T10:00:00Z"),
                        field("trace_id", "链路追踪ID", "varchar", 100, null, null, true, null,
                                "一次请求或任务的链路追踪 ID", "audit", "trace,audit", "request_trace_id",
                                "trace", null, false, "enabled", "trace-001"),
                        field("request_id", "请求ID", "varchar", 100, null, null, true, null,
                                "外部请求 ID 或幂等请求 ID", "audit", "trace,audit", "idempotency_key",
                                "trace", null, false, "enabled", "req-001")
                ),
                List.of(template("审计日志表模板", "适用于操作日志/审计日志表", "audit",
                        customTf("id", "bigint", false, null, "主键 ID", 1, true),
                        tf("created_at", 2, true),
                        tf("created_by", 3, false),
                        tf("updated_at", 4, true),
                        tf("updated_by", 5, false),
                        tf("trace_id", 6, false),
                        tf("request_id", 7, false)))
        );
    }

    private static StarterKitDomain domain(String code, String name, String description) {
        return new StarterKitDomain(code, name, description);
    }

    private static StarterKitEnumDefinition enumDef(
            String code,
            String name,
            String description,
            String valueType,
            StarterKitEnumValue... values
    ) {
        return new StarterKitEnumDefinition(code, name, description, valueType, List.of(values));
    }

    private static StarterKitEnumValue enumValue(String value, String label, int sortOrder) {
        return new StarterKitEnumValue(value, label, sortOrder);
    }

    private static StarterKitFieldDefinition field(
            String name,
            String displayName,
            String dataType,
            Integer length,
            Integer precisionVal,
            Integer scaleVal,
            Boolean nullable,
            String defaultValue,
            String comment,
            String domainCode,
            String tags,
            String aliases,
            String category,
            String codeSetCode,
            Boolean sensitive,
            String status,
            String exampleValue
    ) {
        return new StarterKitFieldDefinition(
                name,
                displayName,
                dataType,
                length,
                precisionVal,
                scaleVal,
                nullable,
                defaultValue,
                comment,
                domainCode,
                tags,
                aliases,
                category,
                codeSetCode,
                sensitive,
                status,
                exampleValue);
    }

    private static StarterKitTemplateDefinition template(
            String name,
            String description,
            String tablePrefix,
            StarterKitTemplateField... fields
    ) {
        return new StarterKitTemplateDefinition(name, description, tablePrefix, List.of(fields));
    }

    private static StarterKitTemplateField tf(String fieldName, int sortOrder, boolean required) {
        return new StarterKitTemplateField(fieldName, null, null, null, null, null, sortOrder, required);
    }

    private static StarterKitTemplateField customTf(
            String name,
            String dataType,
            Boolean nullable,
            String defaultValue,
            String comment,
            int sortOrder,
            boolean required
    ) {
        return new StarterKitTemplateField(null, name, dataType, nullable, defaultValue, comment, sortOrder, required);
    }
}
