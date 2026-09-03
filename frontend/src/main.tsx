import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  App,
  Alert,
  AutoComplete,
  Button,
  Card,
  DatePicker,
  Descriptions,
  Drawer,
  Empty,
  Form,
  Input,
  InputNumber,
  Layout,
  Menu,
  Modal,
  Progress,
  Select,
  Space,
  Switch,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Upload,
  message,
} from "antd";
import { QuestionCircleOutlined } from "@ant-design/icons";
import axios from "axios";
import "antd/dist/reset.css";
const api = axios.create({ baseURL: "/api" });
api.interceptors.request.use((c) => {
  const t = localStorage.getItem("er_token");
  if (t) c.headers.Authorization = `Bearer ${t}`;
  return c;
});
api.interceptors.response.use(
  (r) => r,
  (e) => {
    if (e.response?.status === 401) {
      localStorage.clear();
      location.reload();
    }
    return Promise.reject(e);
  },
);
type User = { account: string; employeeName: string; role: string; roleName?: string; permissions?: string[] };
const roleNames: Record<string, string> = { ADMIN: "系统管理员", OPERATIONS: "运营人员" };
const write = (u: User) => u.role === "ADMIN" || u.role === "OPERATIONS";
function AutoCompleteSales(props:any){const [options,setOptions]=useState<any[]>([]);useEffect(()=>{api.get("/risk-management/organization-risk/sales-managers").then(r=>setOptions((r.data||[]).map((x:any)=>({value:x.employeeName||x.loginAccount})))) .catch(()=>{});},[]);return <AutoComplete {...props} options={options} allowClear placeholder="可选择或直接输入" filterOption={(input,option)=>String(option?.value||'').toLowerCase().includes(input.toLowerCase())}/>;}
const subjectNames: Record<string, string> = {
  GOVERNMENT: "政府单位",
  ENTERPRISE: "企业客户",
  OTHER: "其它机构",
};
const sourceNames: Record<string, string> = {
  RISK_IMPORT: "风险监测导入",
  ARCHAEOLOGY: "考古导入",
  MANUAL: "人工录入",
  SYSTEM: "系统同步",
};
const enterpriseStatusNames: Record<string, string> = {
  UNSCANNED: "未采集",
  COLLECTING: "采集中",
  PENDING_QUOTA: "等待配额",
  SUCCEEDED: "采集成功",
  PARTIAL: "部分成功",
  FAILED: "采集失败",
};
const enterpriseStatusDescriptions: Record<string, string> = {
  UNSCANNED: "企业尚未完成过采集。",
  COLLECTING: "当前存在正在执行的采集任务。",
  PENDING_QUOTA: "当日采集配额耗尽，等待后续配额恢复后继续采集。",
  SUCCEEDED: "本次所有采集项均成功完成。",
  PARTIAL: "部分采集项成功、部分失败，已生成阶段性结果，可继续补采。",
  FAILED: "本次没有任何采集项成功，需要检查原因后重试。",
};
const enterpriseStatusColors: Record<string, string> = {
  UNSCANNED: "default",
  COLLECTING: "processing",
  PENDING_QUOTA: "warning",
  SUCCEEDED: "success",
  PARTIAL: "warning",
  FAILED: "error",
};
const enterpriseStatusOptions = Object.entries(enterpriseStatusNames).map(
  ([value, label]) => ({ value, label }),
);
const enterpriseStatusHelp = (
  <div>
    {Object.entries(enterpriseStatusNames).map(([value, label]) => (
      <div key={value}>
        <span style={{ fontWeight: 600 }}>{label}：</span>
        {enterpriseStatusDescriptions[value]}
      </div>
    ))}
  </div>
);
const batchStatusNames: Record<string, string> = {
  COLLECTING: "采集中",
  PAUSED_QUOTA: "额度暂停",
  SUCCEEDED: "采集成功",
  PARTIAL: "部分成功",
  FAILED: "采集失败",
};
const batchRiskStatusNames: Record<string, string> = {
  NEW: "新增风险",
  CONTINUING: "持续风险",
  UPGRADED: "风险升级",
  RESOLVED: "风险消除",
};
const batchRiskStatusColors: Record<string, string> = {
  NEW: "red",
  CONTINUING: "default",
  UPGRADED: "volcano",
  RESOLVED: "green",
};
const capabilityStatusNames: Record<string, string> = {
  SUCCEEDED: "采集成功",
  FAILED: "采集失败",
  PROVIDER_ERROR: "服务异常",
  UNAVAILABLE: "服务不可用",
};
const riskNames: Record<string, string> = {
  RISK_OVERVIEW: "风险总览",
  DISHONEST: "失信被执行人",
  JUDGMENT_DEBTOR: "被执行人",
  JUDICIAL_CASE: "司法解析",
  CASE_FILING: "立案信息",
  SHELL_COMPANY: "空壳公司识别",
};
const capNames: Record<string, string> = {
  COMPANY_REGISTRATION: "主体信息",
  EQUITY_TREE: "股权关系",
  RISK_OVERVIEW: "风险总览",
  DISHONEST: "失信被执行人",
  JUDGMENT_DEBTOR: "被执行人",
  JUDICIAL_CASE: "司法解析",
  CASE_FILING: "立案信息",
  SHELL_COMPANY: "空壳公司识别",
  BIDDING: "招标信息",
  FINANCING: "融资信息",
  CREDIT_EVALUATION: "信用评价",
};
const levelNames: Record<string, string> = {
  CRITICAL: "严重风险",
  HIGH: "高风险",
  MEDIUM: "中风险",
  LOW: "低风险",
  NONE: "无风险",
  INSUFFICIENT_DATA: "数据不足",
};
const fmt = (v: any) =>
  v ? new Date(v).toLocaleString("zh-CN", { hour12: false }) : "-";
const levelColors: Record<string, string> = {
  CRITICAL: "red",
  HIGH: "volcano",
  MEDIUM: "orange",
  LOW: "green",
  NONE: "green",
  INSUFFICIENT_DATA: "default",
};
const RiskLevelTag = ({ value }: { value: any }) => {
  const raw = String(value || "NONE"),
    [level, count] = raw.split("*");
  return (
    <Tag color={levelColors[level] || "default"}>
      {levelNames[level] || level}
      {count ? "*" + count : ""}
    </Tag>
  );
};
const CollectionStatusTag = ({ value }: { value: any }) => {
  const status = String(value || "");
  const label = enterpriseStatusNames[status] || status || "-";
  return (
    <Tag color={enterpriseStatusColors[status] || "default"}>{label}</Tag>
  );
};
function OverflowText({ value }: { value: any }) {
  const text = value == null || value === "" ? "-" : String(value);
  return (
    <Tooltip title={text}>
      <span
        style={{
          display: "block",
          minWidth: 0,
          overflow: "hidden",
          textOverflow: "ellipsis",
          whiteSpace: "nowrap",
        }}
      >
        {text}
      </span>
    </Tooltip>
  );
}
function Login({ done }: { done: (u: User) => void }) {
  const [f] = Form.useForm();
  return (
    <Layout
      style={{
        minHeight: "100vh",
        justifyContent: "center",
        alignItems: "center",
      }}
    >
      <Card title="企业风控登录" style={{ width: 360 }}>
        <Form
          form={f}
          layout="vertical"
          onFinish={async (v) => {
            try {
              const r = (await api.post("/auth/login", v)).data;
              localStorage.setItem("er_token", r.token);
              localStorage.setItem("er_user", JSON.stringify(r));
              done(r);
            } catch (e: any) {
              message.error(e.response?.data?.message || "登录失败");
            }
          }}
        >
          <Form.Item name="account" label="账号" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true }]}>
            <Input.Password />
          </Form.Item>
          <Button type="primary" htmlType="submit" block>
            登录
          </Button>
        </Form>
      </Card>
    </Layout>
  );
}
function EnterpriseModal({
  open,
  close,
  done,
}: {
  open: boolean;
  close: () => void;
  done: () => void;
}) {
  const [f] = Form.useForm();
  useEffect(() => {
    if (open) {
      f.resetFields();
      f.setFieldsValue({ subjectType: "ENTERPRISE", monitoringEnabled: true });
    }
  }, [open]);
  const dismiss = () => {
    f.resetFields();
    close();
  };
  const save = async (v: any, reactivateDeleted = false) => {
    const payload = {
      ...v,
      monitoringEnabled: v.monitoringEnabled === true,
      reactivateDeleted,
    };
    try {
      await api.post("/risk-management/organization-risk/import", payload);
      message.success(reactivateDeleted ? "已启用历史企业记录" : "新增成功");
      dismiss();
      done();
    } catch (e: any) {
      if (e.response?.status === 409 && !reactivateDeleted) {
        Modal.confirm({
          title: "启用历史企业",
          content: "该企业有历史已删企业，将直接启用原记录。",
          okText: "确认启用",
          cancelText: "取消",
          onOk: () => save(v, true),
        });
        return;
      }
      message.error(e.response?.data?.message || "保存失败");
    }
  };
  return (
    <Modal open={open} title="新增企业" onCancel={dismiss} footer={null}>
      <Form form={f} layout="vertical" onFinish={(v) => save(v)}>
        <Form.Item
          name="companyName"
          label="企业名称"
          rules={[{ required: true }]}
        >
          <Input />
        </Form.Item>
        <Form.Item
          name="unifiedCreditCode"
          label="统一社会信用代码"
          rules={[{ required: true, len: 18 }]}
        >
          <Input />
        </Form.Item>
        <Form.Item
          name="salesManager"
          label="销售经理"
          rules={[{ required: true, message: "请填写销售经理" }]}
        >
          <Input />
        </Form.Item>
        <Form.Item
          name="subjectType"
          label="主体类型"
          rules={[{ required: true }]}
        >
          <Select
            options={[
              ["ENTERPRISE", "企业客户"],
              ["GOVERNMENT", "政府单位"],
              ["OTHER", "其它机构"],
            ].map(([value, label]) => ({ value, label }))}
          />
        </Form.Item>
        <Form.Item
          name="monitoringEnabled"
          label="开启监测"
          valuePropName="checked"
          rules={[{ required: true }]}
        >
          <Switch />
        </Form.Item>
        <Button type="primary" htmlType="submit">
          保存
        </Button>
      </Form>
    </Modal>
  );
}
function IdentityModal({
  item,
  user,
  close,
  done,
}: {
  item: any;
  close: () => void;
  done: () => void;
  user: User;
}) {
  const [f] = Form.useForm();
  useEffect(() => {
    if (item)
      f.setFieldsValue({
        companyName: item.companyName,
        unifiedCreditCode: item.unifiedCreditCode,
        subjectType: item.subjectType,
        salesManager: item.salesManager || user.employeeName,
      });
  }, [item]);
  const save = async (v: any) => {
    try {
      await api.patch(
        `/risk-management/organization-risk/${item.id}/identity`,
        { companyName: v.companyName, unifiedCreditCode: v.unifiedCreditCode, subjectType: v.subjectType, salesManager: v.salesManager },
      );
      message.success("企业信息已更新");
      close();
      done();
    } catch (e: any) {
      message.error(e.response?.data?.message || "更新失败");
    }
  };
  return (
    <Modal open={!!item} title="编辑企业信息" onCancel={close} footer={null}>
      <Form form={f} layout="vertical" onFinish={save}>
        <Form.Item
          name="companyName"
          label="企业名称"
          rules={[{ required: true, message: "请输入企业名称" }]}
        >
          <Input />
        </Form.Item>
        <Form.Item
          name="unifiedCreditCode"
          label="统一社会信用代码"
          rules={[
            { required: true, len: 18, message: "请输入18位统一社会信用代码" },
          ]}
        >
          <Input />
        </Form.Item>
        <Form.Item
          name="salesManager"
          label="销售经理"
          rules={[{ required: true, message: "请填写销售经理" }]}
        >
          <AutoCompleteSales />
        </Form.Item>
        <Form.Item
          name="subjectType"
          label="主体类型"
          rules={[{ required: true }]}
        >
          <Select
            options={[
              ["ENTERPRISE", "企业客户"],
              ["GOVERNMENT", "政府单位"],
              ["OTHER", "其它机构"],
            ].map(([value, label]) => ({ value, label }))}
          />
        </Form.Item>
        <Space>
          <Button onClick={close}>取消</Button>
          <Button type="primary" htmlType="submit">
            保存
          </Button>
        </Space>
      </Form>
    </Modal>
  );
}
function Models({ user }: { user: User }) {
  const [models, setModels] = useState<any[]>([]),
    [type, setType] = useState("ENTERPRISE"),
    [f] = Form.useForm();
  useEffect(() => {
    api.get("/risk-management/models").then((r) => setModels(r.data));
  }, []);
  const fs = models.filter((x) => x.modelType === type);
  useEffect(
    () =>
      f.setFieldsValue({
        factors: fs.map((x) => ({
          factorCode: x.factorCode,
          riskLevel: x.riskLevel,
          enabled: x.enabled,
        })),
      }),
    [models, type],
  );
  const levels = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];
  return (
    <Card title="风险等级">
      <Tabs
        activeKey={type}
        onChange={setType}
        items={Object.keys(subjectNames).map((t) => ({
          key: t,
          label: subjectNames[t],
          children: (
            <Form
              form={f}
              onFinish={async (v) => {
                try {
                  await api.put(`/risk-management/models/${type}`, v);
                  message.success("保存成功");
                  setModels((await api.get("/risk-management/models")).data);
                } catch (e: any) {
                  message.error(e.response?.data?.message || "保存失败");
                }
              }}
            >
              <Table
                pagination={false}
                rowKey="factorCode"
                dataSource={fs}
                columns={[
                  { title: "因素", dataIndex: "factorName" },
                  {
                    title: "风险等级",
                    render: (_: any, x: any, i: number) => (
                      <>
                        <Form.Item name={["factors", i, "factorCode"]} hidden>
                          <Input />
                        </Form.Item>
                        <Form.Item name={["factors", i, "riskLevel"]}>
                          <Select
                            style={{ width: 130 }}
                            options={levels.map((value) => ({
                              value,
                              label: <RiskLevelTag value={value} />,
                            }))}
                          />
                        </Form.Item>
                      </>
                    ),
                  },
                  {
                    title: "启用",
                    render: (_: any, x: any, i: number) => (
                      <Form.Item
                        name={["factors", i, "enabled"]}
                        valuePropName="checked"
                      >
                        <Switch disabled={user.role !== "ADMIN"} />
                      </Form.Item>
                    ),
                  },
                ]}
              />
              {user.role === "ADMIN" && (
                <>
                  <Button type="primary" htmlType="submit">
                    保存配置
                  </Button>
                  <div style={{ marginTop: 16, color: "#666" }}>
                    开启因素如有命中机构，则进行风险提示；机构风险等级取命中因素中的最高等级，同一等级命中多个因素时以“风险等级*n”展示；未命中任何开启因素时为无风险。
                  </div>
                </>
              )}
            </Form>
          ),
        }))}
      />
    </Card>
  );
}
function parseRecords(normalized: any): Record<string, any>[] {
  const s = String(normalized || "");
  try {
    const r = JSON.parse(s).records;
    if (Array.isArray(r)) return r;
  } catch {}
  const m = [
    ...s.matchAll(/rawSummary=([\s\S]*?)(?=\}, \{rawSummary=|\}\]\}$)/g),
  ];
  const text = m[0]?.[1] || s;
  const rows = text
    .split(/\r?\n/)
    .map((x) => x.trim())
    .filter(
      (x) => x.startsWith("|") && x.endsWith("|") && !/^\|[-: |]+\|$/.test(x),
    );
  if (rows.length >= 2) {
    const cells = (x: string) =>
      x
        .slice(1, -1)
        .split("|")
        .map((v) => v.trim());
    const h = cells(rows[0]),
      start = /^\|[-: |]+\|$/.test(rows[1]) ? 2 : 1;
    return rows
      .slice(start)
      .map((x) =>
        Object.fromEntries(cells(x).map((v, i) => [h[i] || `字段${i + 1}`, v])),
      );
  }
  return text ? [{ 原始记录: text }] : [];
}
function RecordTable({
  caps,
  code,
  includeCapability = false,
}: {
  caps: any[];
  code?: string;
  includeCapability?: boolean;
}) {
  if (code === "EQUITY_TREE") return <EquityTree caps={caps} />;
  if (code === "DISHONEST") return <DishonestTable caps={caps} />;
  if (code === "CREDIT_EVALUATION") return <CreditTable caps={caps} />;
  if (code === "SHELL_COMPANY" && hasNoShellTag(caps))
    return <Empty description="未识别到空壳公司标签" />;
  const records = caps
    .filter((x) => !code || x.capability === code)
    .flatMap((x) =>
      parseRecords(x.normalized).map((r, i) => ({
        key: `${x.capability}-${i}`,
        采集项: capNames[x.capability] || x.capability,
        ...r,
      })),
    );
  if (!records.length) return <Empty description="暂无记录" />;
  const keys = Array.from(
    new Set(
      records.flatMap((r) =>
        Object.keys(r).filter(
          (k) => k !== "key" && k !== "采集项" && k !== "详情获取方式",
        ),
      ),
    ),
  );
  return (
    <Table
      size="small"
      pagination={{ pageSize: 10, showSizeChanger: false }}
      dataSource={records}
      columns={[
        ...(includeCapability
          ? [
              {
                title: "采集项",
                dataIndex: "采集项",
                ellipsis: true,
                render: (v: any) => <OverflowText value={v} />,
              },
            ]
          : []),
        ...keys
          .slice(0, 8)
          .map((k) => ({
            title: k === "#" ? "序号" : k,
            dataIndex: k,
            width: k === "#" ? 56 : undefined,
            align: k === "#" ? ("center" as const) : undefined,
            ellipsis: true,
            render: (v: any) => <OverflowText value={v} />,
          })),
      ]}
    />
  );
}
function EquityTree({ caps }: { caps: any[] }) {
  const equity = caps.find((x) => x.capability === "EQUITY_TREE");
  const rows = parseRecords(equity?.normalized);
  const root = rows.find((r) => /^\d+$/.test(String(r["字段"] || "")));
  if (!root) return <Empty description="暂无股权关系" />;
  const name = String(root["值"] || "-"),
    code = String(root["字段3"] || "-"),
    status = String(root["字段4"] || "-"),
    type = String(root["字段6"] || "-"),
    nodes = String(root["字段5"] || "");
  const children = Array.from(
    nodes.matchAll(
      /名称=([^；]+)；统一社会信用代码=([^；]+)；登记状态=([^；]+)；entityType=([^；]+)；持股比例=([^；]+)/g,
    ),
  ).map((m, i) => ({
    key: i,
    name: m[1],
    code: m[2],
    status: m[3],
    type: m[4],
    ratio: m[5],
  }));
  return (
    <>
      <Descriptions bordered size="small" column={2}>
        <Descriptions.Item label="主体名称">{name}</Descriptions.Item>
        <Descriptions.Item label="统一社会信用代码">{code}</Descriptions.Item>
        <Descriptions.Item label="登记状态">{status}</Descriptions.Item>
        <Descriptions.Item label="主体类型">{type}</Descriptions.Item>
      </Descriptions>
      <div style={{ marginTop: 16 }}>
        <div style={{ fontWeight: 600 }}>
          <span
            style={{
              display: "inline-block",
              width: 14,
              height: 14,
              lineHeight: "12px",
              textAlign: "center",
              border: "1px solid #8c8c8c",
              marginRight: 12,
              fontSize: 12,
            }}
          >
            −
          </span>
          {name}
        </div>
        <div style={{ margin: "10px 0 8px 26px", color: "#8c8c8c" }}>
          <Tag color="green">{status}</Tag>
          {code}
        </div>
        {children.length > 0 && (
          <div
            style={{
              marginLeft: 31,
              borderLeft: "1px solid #d9d9d9",
              paddingLeft: 20,
            }}
          >
            {children.map((child) => (
              <div
                key={child.key}
                style={{
                  position: "relative",
                  padding: "5px 0",
                  lineHeight: "24px",
                }}
              >
                <span
                  style={{
                    position: "absolute",
                    left: -20,
                    top: 17,
                    width: 10,
                    borderTop: "1px solid #d9d9d9",
                  }}
                />
                <span>{child.name}</span>
                <Tag color="blue" style={{ marginLeft: 8 }}>
                  持股 {child.ratio}
                </Tag>
                <Tag color="green">{child.status}</Tag>
                <Tag>{child.type}</Tag>
                <span style={{ color: "#8c8c8c" }}>{child.code}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </>
  );
}
function DishonestTable({ caps }: { caps: any[] }) {
  const result = caps.find((x) => x.capability === "DISHONEST");
  const records = parseRecords(result?.normalized).map((r, i) => ({
    key: i,
    ...r,
  }));
  if (!records.length) return <Empty description="暂无记录" />;
  return (
    <Table
      size="small"
      pagination={{ pageSize: 10, showSizeChanger: false }}
      scroll={{ x: 1380 }}
      dataSource={records}
      columns={[
        {
          title: "序号",
          dataIndex: "#",
          width: 56,
          align: "center",
          render: (v: any) => v || "-",
        },
        {
          title: "名称",
          dataIndex: "名称",
          width: 150,
          ellipsis: true,
          render: (v: any) => <OverflowText value={v} />,
        },
        {
          title: "案号",
          dataIndex: "案号",
          width: 210,
          render: (v: any) => (
            <span style={{ whiteSpace: "nowrap" }}>{v || "-"}</span>
          ),
        },
        {
          title: "法院",
          dataIndex: "法院",
          width: 180,
          ellipsis: true,
          render: (v: any) => <OverflowText value={v} />,
        },
        { title: "发布日期", dataIndex: "发布日期", width: 116 },
        { title: "地区", dataIndex: "地区", width: 64, align: "center" },
        { title: "涉案金额", dataIndex: "涉案金额", width: 120 },
        {
          title: "生效法律文书",
          dataIndex: "basisDoc",
          width: 190,
          ellipsis: true,
          render: (v: any) => <OverflowText value={v} />,
        },
        {
          title: "违规性质",
          dataIndex: "disruptTypeName",
          width: 260,
          ellipsis: true,
          render: (v: any) => <OverflowText value={v} />,
        },
      ]}
    />
  );
}
function hasNoShellTag(caps: any[]) {
  const records = parseRecords(
    caps.find((x) => x.capability === "SHELL_COMPANY")?.normalized,
  );
  return (
    !records.length ||
    records.some((r) => r["字段"] === "hasShellTags" && String(r["值"]) === "0")
  );
}
function CreditTable({ caps }: { caps: any[] }) {
  const records = parseRecords(
    caps.find((x) => x.capability === "CREDIT_EVALUATION")?.normalized,
  );
  const headerNames: Record<string, string> = {
    creditLevel: "纳税信用等级",
    evaluationOrg: "评价机构",
    taxType: "税种",
    bondCreditLevel: "债券信用等级",
    ratingOutlook: "评级展望",
    ratingDate: "评级日期",
    ratingCompany: "评级机构",
    subjectRating: "主体评级",
  };
  const groups: { headers: string[]; rows: Record<string, string>[] }[] = [];
  let group: { headers: string[]; rows: Record<string, string>[] } | undefined;
  for (const record of records) {
    if (record["字段"] === "#") {
      const headers = Object.entries(record)
        .filter(
          ([key]) => key === "字段" || key === "值" || /^字段\d+$/.test(key),
        )
        .map(([, value]) => String(value));
      group = { headers, rows: [] };
      groups.push(group);
      continue;
    }
    if (group && /^\d+$/.test(String(record["字段"] || ""))) {
      const values = Object.entries(record)
        .filter(
          ([key]) => key === "字段" || key === "值" || /^字段\d+$/.test(key),
        )
        .map(([, value]) => String(value));
      group.rows.push(
        Object.fromEntries(
          group.headers.map((header, index) => [header, values[index] || "-"]),
        ),
      );
    }
  }
  if (!groups.some((group) => group.rows.length))
    return <Empty description="暂无信用评价记录" />;
  return (
    <>
      {groups
        .filter((group) => group.rows.length)
        .map((group, index) => {
          const title = group.headers.includes("年份")
            ? "纳税信用评价"
            : "债券信用评级";
          return (
            <div
              key={title}
              style={{ marginBottom: index < groups.length - 1 ? 20 : 0 }}
            >
              {groups.length > 1 && (
                <div style={{ fontWeight: 600, marginBottom: 10 }}>{title}</div>
              )}
              <Table
                size="small"
                rowKey={(_, rowIndex) => `${title}-${rowIndex}`}
                pagination={{
                  pageSize: 10,
                  showSizeChanger: false,
                  showTotal: (total) => `共 ${total} 条`,
                }}
                dataSource={group.rows}
                columns={group.headers.map((header) => ({
                  title:
                    header === "#" ? "序号" : headerNames[header] || header,
                  dataIndex: header,
                  width: header === "#" ? 56 : undefined,
                  align: header === "#" ? ("center" as const) : undefined,
                  ellipsis: true,
                  render: (value: any) => <OverflowText value={value} />,
                }))}
              />
            </div>
          );
        })}
    </>
  );
}
function RiskRecords({ caps }: { caps: any[] }) {
  const overview = caps.find((x) => x.capability === "RISK_OVERVIEW");
  const overviewRows = parseRecords(overview?.normalized)
    .filter(
      (r) =>
        r["风险类型"] &&
        r["风险等级"] &&
        r["风险等级"] !== "风险type" &&
        r["风险数量"] !== "等级",
    )
    .map((r, i) => ({
      key: `overview-${i}`,
      type: r["风险类型"],
      level: r["风险等级"],
      count: r["风险数量"],
    }));
  const codes = [
    "DISHONEST",
    "JUDGMENT_DEBTOR",
    "JUDICIAL_CASE",
    "CASE_FILING",
    "SHELL_COMPANY",
  ];
  return (
    <Tabs
      defaultActiveKey="RISK_OVERVIEW"
      items={[
        {
          key: "RISK_OVERVIEW",
          label: "风险总览",
          children: overviewRows.length ? (
            <Table
              size="small"
              pagination={false}
              dataSource={overviewRows}
              columns={[
                { title: "风险类型", dataIndex: "type" },
                {
                  title: "风险等级",
                  dataIndex: "level",
                  render: (v: any) => (
                    <Tag color={String(v).includes("高") ? "red" : "orange"}>
                      {v}
                    </Tag>
                  ),
                },
                { title: "风险数量", dataIndex: "count" },
              ]}
            />
          ) : (
            <Empty description="暂无记录" />
          ),
        },
        ...codes.map((code) => ({
          key: code,
          label: riskNames[code],
          children: <RecordTable caps={caps} code={code} />,
        })),
      ]}
    />
  );
}
function Evidence({ p, evidence }: { p: any; evidence: any }) {
  const rows = (evidence?.factors || []).filter((x: any) => x.factorCode !== "LIMIT_CONSUMPTION").map((x: any) => ({
    ...x,
    key: x.factorCode,
    resultLabel:
      x.result === "TRIGGERED"
        ? "命中并参与预警"
        : x.result === "DISABLED"
          ? "未开启，不参与预警"
          : x.result === "UNAVAILABLE"
            ? "采集异常，未参与判定"
            : "未命中",
    status: x.available ? "采集成功" : "采集异常",
    description:
      x.result === "UNAVAILABLE"
        ? x.errorMessage || "该因素未获得可用采集结果"
        : x.result === "DISABLED"
          ? "该因素已关闭，不参与最终预警"
          : "",
  }));
  return (
    <>
      <Descriptions
        bordered
        size="small"
        column={3}
        labelStyle={{ width: 96, whiteSpace: "nowrap" }}
        style={{ marginBottom: 16 }}
      >
        <Descriptions.Item label="风险预警等级">
          <RiskLevelTag value={p.riskLevel} />
        </Descriptions.Item>
        <Descriptions.Item label="命中因素数">
          {evidence?.hitFactorCount ?? 0}
        </Descriptions.Item>
        <Descriptions.Item label="判定时间">
          {fmt(evidence?.scoredAt)}
        </Descriptions.Item>
      </Descriptions>
      {evidence?.compatibility && (
        <div style={{ marginBottom: 12, color: "#d48806" }}>
          历史记录未保存配置快照，以下内容基于当前配置补全。
        </div>
      )}
      <Table
        pagination={false}
        dataSource={rows}
        columns={[
          {
            title: "风险因素",
            dataIndex: "factorName",
            render: (v: any, x: any) =>
              v || riskNames[x.factorCode] || x.factorCode,
          },
          {
            title: "启用状态",
            dataIndex: "enabled",
            render: (v: any) => (
              <Tag color={v ? "green" : "default"}>
                {v ? "已开启" : "已关闭"}
              </Tag>
            ),
          },
          {
            title: "配置风险等级",
            render: (_: any, x: any) => (
              <RiskLevelTag value={x.configuredRiskLevel || x.riskLevel} />
            ),
          },
          {
            title: "采集状态",
            dataIndex: "status",
            render: (v: any, x: any) => (
              <Tag color={x.available ? "green" : "red"}>{v}</Tag>
            ),
          },
          {
            title: "记录数",
            dataIndex: "recordCount",
            render: (v: any) => v ?? 0,
          },
          {
            title: "判定结果",
            dataIndex: "resultLabel",
            render: (v: any, x: any) => (
              <Tag
                color={
                  x.result === "TRIGGERED"
                    ? "red"
                    : x.result === "UNAVAILABLE"
                      ? "orange"
                      : "default"
                }
              >
                {v}
              </Tag>
            ),
          },
          {
            title: "说明",
            dataIndex: "description",
            ellipsis: true,
            render: (v: any) => <OverflowText value={v || "-"} />,
          },
        ]}
      />
    </>
  );
}
function CapabilityStatus({ caps }: { caps: any[] }) {
  return (
    <Table
      pagination={false}
      dataSource={caps.filter((x) => x.capability !== "LIMIT_CONSUMPTION").map((x) => ({
        ...x,
        key: x.capability,
        item: capNames[x.capability] || x.capability,
      }))}
      columns={[
        {
          title: "采集项",
          dataIndex: "item",
          ellipsis: true,
          render: (v: any) => <OverflowText value={v} />,
        },
        {
          title: "状态",
          render: (_: any, x: any) => (
            <span>
              <i
                style={{
                  display: "inline-block",
                  width: 7,
                  height: 7,
                  borderRadius: "50%",
                  background:
                    x.status === "SUCCEEDED"
                      ? "#52c41a"
                      : x.status === "FAILED"
                        ? "#ff4d4f"
                        : "#faad14",
                  marginRight: 7,
                }}
              />
              {capabilityStatusNames[x.status] || x.status}
            </span>
          ),
        },
        {
          title: "记录数",
          dataIndex: "recordCount",
          render: (v: any) => v ?? 0,
        },
        { title: "命中", render: (_: any, x: any) => (x.hit ? "是" : "否") },
        {
          title: "错误",
          dataIndex: "errorMessage",
          ellipsis: true,
          render: (v: any) => <OverflowText value={v} />,
        },
        { title: "采集时间", dataIndex: "collectedAt", render: fmt },
      ]}
    />
  );
}
function Detail({ detail }: { detail: any }) {
  const p = detail.profile || {},
    b = detail.basicDetails || {},
    caps = detail.capabilities || [],
    evidence = detail.warningEvidence;
  const detailLabelStyle: React.CSSProperties = { width: 112 };
  const detailContentStyle: React.CSSProperties = {
    whiteSpace: "normal",
    overflowWrap: "anywhere",
  };
  return (
    <Tabs
      items={[
        {
          key: "base",
          label: "主体信息",
          children: (
            <>
              <Card title="查询信息">
                <Descriptions bordered column={2}>
                  <Descriptions.Item label="企业名称">
                    {p.companyName || "-"}
                  </Descriptions.Item>
                  <Descriptions.Item label="统一社会信用代码">
                    {p.unifiedCreditCode || "-"}
                  </Descriptions.Item>
                  <Descriptions.Item label="来源">
                    {sourceNames[p.source] || p.source || "-"}
                  </Descriptions.Item>
                  <Descriptions.Item label="企业类型">
                    {subjectNames[p.subjectType] || p.subjectType || "-"}
                  </Descriptions.Item>
                  <Descriptions.Item label="最新采集时间">
                    {fmt(p.latestSuccessAt)}
                  </Descriptions.Item>
                  <Descriptions.Item label="采集状态">
                    <CollectionStatusTag value={p.collectionStatus} />
                  </Descriptions.Item>
                  <Descriptions.Item label="风险预警等级">
                    <RiskLevelTag value={p.riskLevel} />
                  </Descriptions.Item>
                  <Descriptions.Item label="监测状态">
                    {p.monitoringEnabled ? "开启" : "关闭"}
                  </Descriptions.Item>
                </Descriptions>
              </Card>
              <Card title="标签展示" style={{ marginTop: 16 }}>
                {caps
                  .filter((x: any) => x.capability !== "LIMIT_CONSUMPTION" && x.hit)
                  .map((x: any) => (
                    <Tag
                      key={x.capability}
                      color={
                        [
                          "DISHONEST",
                          "JUDGMENT_DEBTOR",
                          "SHELL_COMPANY",
                        ].includes(x.capability)
                          ? "red"
                          : "orange"
                      }
                    >
                      {capNames[x.capability]}
                    </Tag>
                  ))}
                {!caps.some((x: any) => x.capability !== "LIMIT_CONSUMPTION" && x.hit) && (
                  <Tag color="green">暂无风险标签</Tag>
                )}
              </Card>
              <Card title="基础明细" style={{ marginTop: 16 }}>
                <Descriptions
                  bordered
                  column={2}
                  labelStyle={detailLabelStyle}
                  contentStyle={detailContentStyle}
                >
                  <Descriptions.Item label="登记名称">
                    {b["企业名称"] || p.companyName || "-"}
                  </Descriptions.Item>
                  <Descriptions.Item label="法定代表人">
                    {b["法定代表人"] || "-"}
                  </Descriptions.Item>
                  <Descriptions.Item label="企业类型">
                    {b["企业类型"] || "-"}
                  </Descriptions.Item>
                  <Descriptions.Item label="行业">
                    {b["行业"] || "-"}
                  </Descriptions.Item>
                  <Descriptions.Item label="实缴资本">
                    {b["实缴资本"] || "-"}
                  </Descriptions.Item>
                  <Descriptions.Item label="成立日期">
                    {b["成立日期"] || b["核准日期"] || "-"}
                  </Descriptions.Item>
                  <Descriptions.Item label="参保人数">
                    {b["参保人数"] || "-"}
                  </Descriptions.Item>
                  <Descriptions.Item label="登记状态">
                    {b["登记状态"] || "-"}
                  </Descriptions.Item>
                  <Descriptions.Item label="注册地址" span={2}>
                    {b["注册地址"] ||
                      [b["城市"], b["区县"]].filter(Boolean).join(" ") ||
                      "-"}
                  </Descriptions.Item>
                  <Descriptions.Item label="经营范围" span={2}>
                    {b["经营范围"] || "-"}
                  </Descriptions.Item>
                </Descriptions>
              </Card>
            </>
          ),
        },
        {
          key: "risk",
          label: "风险记录",
          children: <RiskRecords caps={caps} />,
        },
        {
          key: "equity",
          label: "股权关系",
          children: (
            <RecordTable caps={caps} code="EQUITY_TREE" includeCapability />
          ),
        },
        {
          key: "bidding",
          label: "招标与融资",
          children: (
            <RecordTable
              caps={caps.filter((x: any) =>
                ["BIDDING", "FINANCING"].includes(x.capability),
              )}
              includeCapability
            />
          ),
        },
        {
          key: "credit",
          label: "信用评价",
          children: (
            <RecordTable
              caps={caps}
              code="CREDIT_EVALUATION"
              includeCapability
            />
          ),
        },
        {
          key: "evidence",
          label: "评分证据",
          children: <Evidence p={p} evidence={evidence} />,
        },
        {
          key: "status",
          label: "本次能力状态",
          children: <CapabilityStatus caps={caps} />,
        },
      ]}
    />
  );
}
function BatchModal({
  open,
  close,
  done,
  user,
}: {
  open: boolean;
  close: () => void;
  done: () => void;
  user: User;
}) {
  const [preview, setPreview] = useState<any>(),
    [schedule, setSchedule] = useState<any>(),
    [progress, setProgress] = useState<any>(),
    [run, setRun] = useState<any>(),
    [executing, setExecuting] = useState(false),
    [f] = Form.useForm();
  const refreshPreview = () => api.get("/risk-management/collection-runs/preview").then((r) => setPreview(r.data));
  const refreshProgress = () => api.get("/risk-management/collection-progress").then((r) => setProgress(r.data)).catch(() => {});
  const poll = (id: number) => api.get(`/risk-management/collection-runs/${id}`).then((r) => { setRun(r.data); if (r.data.status === "COLLECTING") window.setTimeout(() => poll(id), 1000); else { setExecuting(false); void refreshPreview(); void refreshProgress(); done(); message.success(`批次 ${r.data.batchNo} ${r.data.status === "PAUSED_QUOTA" ? "已暂停，次日自动续跑" : r.data.status === "SUCCEEDED" ? "执行完成" : "执行结束"}`); } }).catch(() => setExecuting(false));
  useEffect(() => {
    if (open) {
      void refreshPreview();
      void refreshProgress();
      const timer = window.setInterval(() => { if (!executing) { void refreshPreview(); void refreshProgress(); } }, 5000);
      api.get("/risk-management/collection-schedule").then((r) => {
        setSchedule(r.data);
        f.setFieldsValue(r.data);
      });
      api.get("/risk-management/collection-runs").then((r) => {
        const active = r.data.find((x: any) => x.status === "COLLECTING");
        if (active) { setRun(active); setExecuting(true); poll(active.id); }
      });
      return () => window.clearInterval(timer);
    }
  }, [open, executing]);
  const cadenceDescription = (
    <div>
      <div>批量采集仅获取风险总览及当前主体模型已启用的风险因素</div>
      <div>企业主体信息、股权关系、招标、融资、信用评价等信息请通过单企业“补全采集”获取</div>
      <div>严重风险 / 高风险 / 中风险：每 7 天至少自动采集一次</div>
      <div>低风险 / 无风险：每 15 天至少自动采集一次</div>
      <div>数据不足：每 15 天进入补采或复核周期；首次未成功采集的企业立即进入待采集队列</div>
      <div>监测未开启或已有正在执行任务的企业暂不执行</div>
    </div>
  );
  const automationRuleDescription = (
    <div>
      <div>* 批量内容：风险总览 + 当前主体模型已启用的风险因素</div>
      <div>* 其它信息：由操作人通过企业列表的“补全采集”执行全量采集</div>
      <div>* 定时执行：每日/每周任务固定23:00触发</div>
      <div>* 采集周期：严重/高/中风险企业每7天至少采集1次，低/无风险企业每15天至少采集1次</div>
      <div>* 异常续跑：当日额度耗尽后剩余企业自动顺延至下一个配置时段续跑；首次采集失败立即入待采队列，数据不足每15天进入补采复核</div>
      <div>* 排除规则：监测未开启、已有执行中任务的企业暂不调度</div>
    </div>
  );
  const execute = async () => {
    try {
      setExecuting(true);
      await refreshPreview();
      const r = (await api.post("/risk-management/collection-runs/execute")).data;
      setRun(r);
      message.success(`批次 ${r.batchNo} 已创建，开始采集`);
      poll(r.runId);
    } catch (e: any) {
      setExecuting(false);
      message.error(e.response?.data?.message || "执行失败");
    }
  };
  return (
    <Modal
      open={open}
      title="批量采集"
      onCancel={close}
      footer={null}
      width={680}
    >
      <Tabs
        items={[
          {
            key: "auto",
            label: "自动化采集",
            children: (
              <Form
                form={f}
                layout="vertical"
                onFinish={async (v) => {
                  try {
                    await api.put("/risk-management/collection-schedule", { ...v, runTime: "23:00" });
                    setSchedule({ ...schedule, ...v, runTime: "23:00" });
                    message.success("自动化规则已保存");
                  } catch (e: any) {
                    message.error(e.response?.data?.message || "保存失败");
                  }
                }}
              >
                <Form.Item
                  name="enabled"
                  label="启用自动化采集"
                  valuePropName="checked"
                >
                  <Switch disabled={!write(user)} />
                </Form.Item>
                <Form.Item
                  name="frequency"
                  label="频率"
                  rules={[{ required: true }]}
                >
                  <Select
                    options={[
                      { value: "DAILY", label: "每日" },
                      { value: "WEEKLY", label: "每周" },
                    ]}
                  />
                </Form.Item>
                <Form.Item
                  noStyle
                  shouldUpdate={(a, b) => a.frequency !== b.frequency}
                >
                  {() =>
                    f.getFieldValue("frequency") === "WEEKLY" ? (
                      <Form.Item
                        name="dayOfWeek"
                        label="每周执行日"
                        rules={[{ required: true }]}
                      >
                        <Select
                          options={[1, 2, 3, 4, 5, 6, 7].map((x) => ({
                            value: x,
                            label: `星期${["一", "二", "三", "四", "五", "六", "日"][x - 1]}`,
                          }))}
                        />
                      </Form.Item>
                    ) : null
                  }
                </Form.Item>
                <Alert
                  type="info"
                  showIcon
                  message="自动化执行规则"
                  description={automationRuleDescription}
                  style={{ marginBottom: 16 }}
                />
                <Button
                  type="primary"
                  htmlType="submit"
                  disabled={!write(user)}
                >
                  保存规则
                </Button>
              </Form>
            ),
          },
          {
            key: "now",
            label: "立即批量采集",
            children: (
              <>
                <Descriptions bordered column={1} size="small">
                  <Descriptions.Item label="立即采集范围">
                    {preview?.scopeDescription || "-"}
                  </Descriptions.Item>
                  <Descriptions.Item label="符合条件企业数">
                    {preview?.eligibleCount ?? "-"} 家
                  </Descriptions.Item>
                  <Descriptions.Item label="今日剩余配额">
                    {preview?.remainingQuota ?? "-"}
                  </Descriptions.Item>
                  <Descriptions.Item label="今日预计批量采集">
                    {preview?.estimatedFullCapacity ?? "-"} 家
                  </Descriptions.Item>
                  <Descriptions.Item label="待执行总量">
                    {progress?.dueCount ?? "-"} 家，预计 {progress?.estimatedDays ?? "-"} 天
                  </Descriptions.Item>
                </Descriptions>
                <Alert
                  type="info"
                  showIcon
                  message="批量采集周期规则"
                  description={cadenceDescription}
                  style={{ marginBottom: 16 }}
                />
                <Button
                  type="primary"
                  onClick={execute}
                  loading={executing}
                  disabled={executing || !write(user)}
                >
                  {executing ? "采集中" : "立即执行"}
                </Button>
                {run && (() => { const critical = Number(run.criticalCount || 0), high = Number(run.highCount || 0), medium = Number(run.mediumCount || 0), low = Number(run.lowCount || 0); const hasRed = critical + high > 0, hasOrange = medium + low > 0; const type = run.status === "COLLECTING" ? "info" : run.status === "PAUSED_QUOTA" ? "warning" : hasRed ? "error" : hasOrange ? "warning" : run.status === "FAILED" ? "error" : "success"; const batchNo = run.batchNo || run.batchno || "-"; const total = Number(run.totalCount ?? run.totalcount ?? 0), success = Number(run.successCount ?? run.successcount ?? 0), partial = Number(run.partialCount ?? run.partialcount ?? 0), failed = Number(run.failureCount ?? run.failurecount ?? 0); return <Alert style={{ marginTop: 16 }} type={type} message={`批次 ${batchNo}：${run.status === "COLLECTING" ? "正在采集" : run.status === "PAUSED_QUOTA" ? "额度已用完，次日自动续跑" : run.status === "SUCCEEDED" ? "采集完成" : run.status === "PARTIAL" ? "采集完成（部分成功）" : "采集失败"}`} description={<><Progress percent={total ? Math.round((success + partial + failed) * 100 / total) : 100} status={run.status === "COLLECTING" ? "active" : run.status === "FAILED" ? "exception" : "normal"} /><div>成功 {success} · 部分成功 {partial} · 失败 {failed} / 共 {total}</div><Space size={6} style={{ marginTop: 8 }}>{critical > 0 && <Tag color="red">严重风险 {critical}</Tag>}{high > 0 && <Tag color="volcano">高风险 {high}</Tag>}{medium > 0 && <Tag color="orange">中风险 {medium}</Tag>}{low > 0 && <Tag color="green">低风险 {low}</Tag>}</Space></>} />; })()}
              </>
            ),
          },
        ]}
      />
    </Modal>
  );
}
function ImportModal({
  open,
  close,
  done,
}: {
  open: boolean;
  close: () => void;
  done: () => void;
}) {
  const upload = async (o: any, reactivateDeleted = false) => {
    const form = new FormData();
    form.append("file", o.file);
    try {
      const url = `/risk-management/organization-risk/import-file${reactivateDeleted ? "?reactivateDeleted=true" : ""}`;
      const r = (await api.post(url, form)).data;
      if (r.requiresReactivationConfirmation && !reactivateDeleted) {
        Modal.confirm({
          title: "启用历史企业",
          content: `共 ${r.historicalDeletedCount} 条历史已删企业，将直接启用原记录。`,
          okText: "确认启用",
          cancelText: "取消",
          onOk: () => upload(o, true),
        });
        return;
      }
      message.success(`导入完成：成功 ${r.success} 条，失败 ${r.failed} 条`);
      if (r.failed)
        message.warning(
          r.errors
            .map((x: any) => `第${x.row ?? "-"}行：${x.message}`)
            .join("；"),
        );
      done();
      o.onSuccess(r);
    } catch (e: any) {
      message.error(e.response?.data?.message || "导入失败");
      o.onError(e);
    }
  };
  return (
    <Modal
      open={open}
      title="批量导入企业"
      onCancel={close}
      footer={<Button onClick={close}>关闭</Button>}
    >
      <p>模板列：企业名称、统一社会信用代码、销售经理、主体类型（为空默认为企业客户）。</p>
      <Button
        href="/api/risk-management/organization-risk/import-template"
        style={{ marginBottom: 12 }}
      >
        下载 Excel 模板
      </Button>
      <br />
      <Upload accept=".xlsx" maxCount={1} customRequest={(o) => upload(o)}>
        <Button type="primary">选择并上传 .xlsx 文件</Button>
      </Upload>
    </Modal>
  );
}
function RunHistory() {
  const [rows, setRows] = useState<any[]>([]);
  const load = () =>
    api.get("/risk-management/collection-runs").then((r) =>
      setRows((r.data || []).map((x: any) => ({ ...x, id: x.id ?? x.ID }))),
    );
  useEffect(() => {
    void load();
  }, []);
  return (
    <Card
      title="采集批次历史"
      style={{ marginTop: 16 }}
      extra={<Button onClick={load}>刷新</Button>}
    >
      <Table
        size="small"
        pagination={{ pageSize: 5, showSizeChanger: false }}
        rowKey="id"
        dataSource={rows}
        columns={[
          {
            title: "采集批次号",
            dataIndex: "batchno",
            render: (v: any, x: any) => v || x.batchNo,
          },
          {
            title: "触发方式",
            dataIndex: "triggertype",
            render: (v: any, x: any) =>
              (v || x.triggerType) === "SCHEDULED" ? "自动化采集" : "立即采集",
          },
          { title: "状态", dataIndex: "status", render: (v: any) => batchStatusNames[v] || v },
          {
            title: "企业数",
            dataIndex: "totalcount",
            render: (v: any, x: any) => v ?? x.totalCount,
          },
          {
            title: "成功",
            dataIndex: "successcount",
            render: (v: any, x: any) => v ?? x.successCount,
          },
          {
            title: "部分成功",
            dataIndex: "partialcount",
            render: (v: any, x: any) => v ?? x.partialCount,
          },
          {
            title: "失败",
            dataIndex: "failurecount",
            render: (v: any, x: any) => v ?? x.failureCount,
          },
          {
            title: "创建时间",
            dataIndex: "createdat",
            render: (v: any, x: any) => fmt(v || x.createdAt),
          },
        ]}
      />
    </Card>
  );
}
function Risk({ user }: { user: User }) {
  const [rows, setRows] = useState<any[]>([]),
    [open, setOpen] = useState(false),
    [editing, setEditing] = useState<any>(),
    [importOpen, setImportOpen] = useState(false),
    [batchOpen, setBatchOpen] = useState(false),
    [detail, setDetail] = useState<any>(),
    [scanning, setScanning] = useState<number[]>([]),
    [exporting, setExporting] = useState(false),
    [f] = Form.useForm();
  const query = (v: any) => {
    const { latestSuccessRange, ...filters } = v || {};
    const [start, end] = latestSuccessRange || [];
    return {
      ...filters,
      latestSuccessStart: start?.startOf("day").toISOString(),
      latestSuccessEnd: end?.add(1, "day").startOf("day").toISOString(),
    };
  };
  const load = async (v?: any) =>
    setRows(
      (
        await api.get("/risk-management/organization-risk", {
          params: query(v || f.getFieldsValue()),
        })
      ).data.map((x: any) => ({
        ...x,
        id: x.id ?? x.ID,
        source: x.source ?? x.SOURCE,
        coverage: x.coverage ?? x.COVERAGE,
      })),
    );
  const exportCurrent = async () => {
    setExporting(true);
    try {
      const response = await api.get("/risk-management/organization-risk/export", {
        params: query(f.getFieldsValue()),
        responseType: "blob",
      });
      const url = URL.createObjectURL(response.data);
      const link = document.createElement("a");
      link.href = url;
      link.download = "企业风控列表.csv";
      link.click();
      URL.revokeObjectURL(url);
    } catch (e: any) {
      message.error("导出失败");
    } finally {
      setExporting(false);
    }
  };
  useEffect(() => {
    void load({});
  }, []);
  const scan = async (id: number) => {
    setScanning((x) => [...x, id]);
    try {
      const result = (await api.post(`/risk-management/organization-risk/${id}/scan`)).data;
      message.success(result?.status === "PARTIAL" ? "采集完成（部分成功，待补采）" : result?.status === "PENDING_QUOTA" ? "额度不足，已进入待配额队列" : "采集完成");
    } catch (e: any) {
      message.error(e.response?.data?.message || "采集失败");
    } finally {
      setScanning((x) => x.filter((v) => v !== id));
      await load(f.getFieldsValue());
    }
  };
  const remove = (x: any) =>
    Modal.confirm({
      title: "删除企业",
      content: `确认删除“${x.companyName}”吗？删除后将从列表隐藏，可在后续新增或导入同一企业时重新启用原记录。`,
      okText: "删除",
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await api.delete(`/risk-management/organization-risk/${x.id}`);
          message.success("已删除企业记录");
          load();
        } catch (e: any) {
          message.error(e.response?.data?.message || "删除失败");
          throw e;
        }
      },
    });
  return (
    <>
      <Card
        title="企业风控列表"
        extra={
          <Space>
            <Button loading={exporting} onClick={exportCurrent}>导出当前结果</Button>
            {write(user) && (
              <>
                <Button type="primary" onClick={() => setOpen(true)}>
                  新增企业
                </Button>
                <Button onClick={() => setImportOpen(true)}>批量导入</Button>
                <Button onClick={() => setBatchOpen(true)}>批量采集</Button>
              </>
            )}
          </Space>
        }
      >
        <Form
          form={f}
          layout="inline"
          style={{ marginBottom: 16, rowGap: 8 }}
          onFinish={(v) => load(v)}
        >
          <Form.Item name="companyName">
            <Input style={{ width: 190 }} placeholder="企业名称" />
          </Form.Item>
          <Form.Item name="unifiedCreditCode">
            <Input style={{ width: 220 }} placeholder="统一社会信用代码" />
          </Form.Item>
          <Form.Item name="salesManager">
            <Input style={{ width: 170 }} placeholder="销售经理" />
          </Form.Item>
          <Form.Item name="batchNo">
            <Input style={{ width: 190 }} placeholder="采集批次号" />
          </Form.Item>
          <Form.Item name="riskLevel">
            <Select
              allowClear
              placeholder="风险等级"
              style={{ width: 140 }}
              options={Object.entries(levelNames).map(([value, label]) => ({
                value,
                label,
              }))}
            />
          </Form.Item>
          <Form.Item name="collectionStatus">
            <Select
              allowClear
              placeholder="采集状态"
              style={{ width: 140 }}
              options={enterpriseStatusOptions}
            />
          </Form.Item>
          <Form.Item name="batchRiskStatus">
            <Select
              allowClear
              placeholder="风险异动类型"
              style={{ width: 140 }}
              options={Object.entries(batchRiskStatusNames)
                .filter(([value]) => value !== "RESOLVED")
                .map(([value, label]) => ({ value, label }))}
            />
          </Form.Item>
          <Tooltip title={enterpriseStatusHelp} placement="topLeft">
            <QuestionCircleOutlined
              aria-label="采集状态说明"
              style={{ color: "#8c8c8c", cursor: "help", marginRight: 8 }}
            />
          </Tooltip>
          <Form.Item name="latestSuccessRange">
            <DatePicker.RangePicker
              placeholder={["最近成功开始", "最近成功结束"]}
            />
          </Form.Item>
          <Button type="primary" htmlType="submit">
            查询
          </Button>
          <Button
            onClick={() => {
              f.resetFields();
              load({});
            }}
          >
            重置
          </Button>
        </Form>
        <Table
          rowKey="id"
          dataSource={rows}
          pagination={{
            defaultPageSize: 10,
            showSizeChanger: true,
            pageSizeOptions: ["10", "20", "50", "100"],
            showTotal: (total) => `共 ${total} 条`,
            locale: { items_per_page: "页" },
          }}
          columns={[
            {
              title: "企业名称",
              dataIndex: "companyName",
              render: (v: any, x: any) =>
                write(user) ? (
                  <Button
                    type="link"
                    style={{ padding: 0 }}
                    onClick={() => setEditing(x)}
                  >
                    {v}
                  </Button>
                ) : (
                  v
                ),
            },
            { title: "统一社会信用代码", dataIndex: "unifiedCreditCode" },
            { title: "销售经理", dataIndex: "salesManager" },
            { title: "主体类型", dataIndex: "subjectType", render: (v: any) => subjectNames[v] || v || subjectNames.ENTERPRISE },
            {
              title: "监测",
              render: (_: any, x: any) => (
                <Switch
                  checked={x.monitoringEnabled}
                  disabled={!write(user)}
                  onChange={(v) =>
                    api
                      .patch(
                        `/risk-management/organization-risk/${x.id}/monitoring`,
                        { enabled: v },
                      )
                      .then(() => load())
                  }
                />
              ),
            },
            {
              title: "采集状态",
              render: (_: any, x: any) => (
                <CollectionStatusTag value={x.collectionStatus} />
              ),
            },
            {
              title: "风险等级",
              render: (_: any, x: any) => <RiskLevelTag value={x.riskLevel} />,
            },
            {
              title: "采集批次号",
              dataIndex: "latestCollectionBatchNo",
              render: (v: any) => v || "-",
            },
            {
              title: "风险异动类型",
              dataIndex: "batchRiskStatus",
              render: (v: any) =>
                v ? <Tag color={batchRiskStatusColors[v]}>{batchRiskStatusNames[v] || v}</Tag> : "-",
            },
            {
              title: "最近成功时间",
              dataIndex: "latestSuccessAt",
              render: (v: any, x: any) => fmt(x.batchCollectedAt || v),
            },
            {
              title: "操作",
              width: 230,
              render: (_: any, x: any) => (
                <Space>
                  <Button
                    type="primary"
                    style={{ width: 82 }}
                    loading={scanning.includes(x.id)}
                    disabled={
                      !write(user) ||
                      !x.monitoringEnabled ||
                      scanning.includes(x.id)
                    }
                    onClick={() => scan(x.id)}
                  >
                    {scanning.includes(x.id)
                      ? "采集中"
                      : x.collectionStatus && x.collectionStatus !== "UNSCANNED"
                        ? "补全采集"
                        : "采集"}
                  </Button>
                  <Button
                    style={{ width: 56 }}
                    onClick={() =>
                      api
                        .get(
                          `/risk-management/organization-risk/${x.id}/snapshot`,
                        )
                        .then((r) => setDetail(r.data))
                    }
                  >
                    详情
                  </Button>
                  {write(user) && (
                    <Button
                      danger
                      style={{ width: 56 }}
                      onClick={() => remove(x)}
                    >
                      删除
                    </Button>
                  )}
                </Space>
              ),
            },
          ]}
        />
      </Card>
      <EnterpriseModal
        open={open}
        close={() => setOpen(false)}
        done={() => load()}
      />
      <IdentityModal
        item={editing}
        user={user}
        close={() => setEditing(undefined)}
        done={() => load()}
      />
      <ImportModal
        open={importOpen}
        close={() => setImportOpen(false)}
        done={() => load()}
      />
      <BatchModal
        open={batchOpen}
        close={() => setBatchOpen(false)}
        done={() => load()}
        user={user}
      />
      <Drawer
        title="企业风控详情"
        open={!!detail}
        onClose={() => setDetail(undefined)}
        width={1080}
      >
        {detail && <Detail detail={detail} />}
      </Drawer>
    </>
  );
}
function Keys({ user }: { user: User }) {
  const [rows, setRows] = useState<any[]>([]),
    [open, setOpen] = useState(false),
    [editing, setEditing] = useState<any>(),
    [f] = Form.useForm();
  const load = () =>
    api.get("/basic-data/tianyancha-mcp-keys").then((r) =>
      setRows((r.data || []).map((x: any) => ({ ...x, id: x.id ?? x.ID }))),
    );
  useEffect(() => {
    void load();
  }, []);
  const edit = (x?: any) => {
    setEditing(x);
    f.setFieldsValue(
      x ? { name: x.name, dailyLimit: x.dailyLimit } : { dailyLimit: 600 },
    );
    setOpen(true);
  };
  const save = async (v: any) => {
    try {
      if (editing)
        await api.put(`/basic-data/tianyancha-mcp-keys/${editing.id}`, v);
      else await api.post("/basic-data/tianyancha-mcp-keys", v);
      message.success("保存成功");
      setOpen(false);
      f.resetFields();
      load();
    } catch (e: any) {
      message.error(e.response?.data?.message || "保存失败");
    }
  };
  const status = (x: any) => {
    const next = !x.enabled;
    Modal.confirm({
      title: next ? "启用 Key" : "停用 Key",
      content: `确认${next ? "启用" : "停用"}“${x.name}”吗？`,
      onOk: async () => {
        try {
          await api.post(`/basic-data/tianyancha-mcp-keys/${x.id}/status`, {
            enabled: next,
          });
          message.success("状态已更新");
          load();
        } catch (e: any) {
          message.error(e.response?.data?.message || "操作失败");
        }
      },
    });
  };
  const remove = (x: any) =>
    Modal.confirm({
      title: "删除 Key",
      content: `确认删除“${x.name}”吗？`,
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await api.delete(`/basic-data/tianyancha-mcp-keys/${x.id}`);
          message.success("已删除");
          load();
        } catch (e: any) {
          message.error(e.response?.data?.message || "删除失败");
        }
      },
    });
  const statusTag = (x: any) => {
    const s = x.dailyStatus || (!x.enabled ? "DISABLED" : "AVAILABLE");
    return s === "DISABLED" ? (
      <Tag>已停用</Tag>
    ) : s === "EXHAUSTED" ? (
      <Tag color="orange">配额已用尽</Tag>
    ) : (
      <Tag color="green">可用</Tag>
    );
  };
  return (
    <Card
      title="风控密钥"
      extra={
        user.role === "ADMIN" && (
          <Button type="primary" onClick={() => edit()}>
            新增 Key
          </Button>
        )
      }
    >
      <Table
        rowKey="id"
        dataSource={rows}
        pagination={{
          showSizeChanger: false,
          showTotal: (t) => `共 ${t} 个天眼查Key`,
        }}
        columns={[
          { title: "名称", dataIndex: "name" },
          { title: "Key", dataIndex: "maskedKey" },
          { title: "当日状态", render: (_: any, x: any) => statusTag(x) },
          { title: "配额限制数", dataIndex: "dailyLimit" },
          {
            title: "已使用配额",
            dataIndex: "usageCount",
            render: (v: any) => v ?? 0,
          },
          { title: "最后使用时间", dataIndex: "lastUsedAt", render: fmt },
          {
            title: "失败原因",
            dataIndex: "failureReason",
            render: (v: any) => <OverflowText value={v} />,
            ellipsis: true,
          },
          {
            title: "操作",
            render: (_: any, x: any) =>
              user.role === "ADMIN" ? (
                <Space>
                  <Button type="link" onClick={() => edit(x)}>
                    编辑
                  </Button>
                  <Button type="link" onClick={() => status(x)}>
                    {x.enabled ? "停用" : "启用"}
                  </Button>
                  <Button type="link" danger onClick={() => remove(x)}>
                    删除
                  </Button>
                </Space>
              ) : (
                <span>-</span>
              ),
          },
        ]}
      />
      <Modal
        open={open}
        title={editing ? "编辑 Key" : "新增 Key"}
        onCancel={() => {
          setOpen(false);
          f.resetFields();
        }}
        footer={null}
      >
        <Form form={f} layout="vertical" onFinish={save}>
          <Form.Item
            name="name"
            label="名称"
            rules={[{ required: true, message: "请输入名称" }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="accessKey"
            label="Key"
            rules={[{ required: !editing, message: "请输入 Key" }]}
          >
            <Input.Password placeholder={editing ? "留空则保留原 Key" : ""} />
          </Form.Item>
          <Form.Item
            name="dailyLimit"
            label="配额限制数"
            rules={[
              { required: true },
              { type: "number", min: 1, message: "请输入正整数" },
            ]}
          >
            <InputNumber style={{ width: "100%" }} min={1} precision={0} />
          </Form.Item>
          <Space>
            <Button onClick={() => setOpen(false)}>取消</Button>
            <Button type="primary" htmlType="submit">
              保存
            </Button>
          </Space>
        </Form>
      </Modal>
    </Card>
  );
}
function Robot({ user }: { user: User }) {
  const [f] = Form.useForm();
  const [c, setC] = useState<any>();
  const selectedLevels = Form.useWatch("riskLevels", f) || ["CRITICAL", "HIGH"];
  const load = () =>
    api.get("/risk-management/wecom-robot").then((r) => {
      setC(r.data);
      f.setFieldsValue({
        enabled: r.data.enabled,
        webhook: r.data.webhook || undefined,
        riskLevels: r.data.riskLevels || ["CRITICAL", "HIGH"],
      });
    });
  useEffect(() => {
    void load();
  }, []);
  return (
    <Card
      title="企业微信群机器人"
      extra={
        c?.configured ? (
          <Tag color="green">已配置 Webhook</Tag>
        ) : (
          <Tag>未配置</Tag>
        )
      }
    >
      <Form
        form={f}
        layout="vertical"
        onFinish={async (v) => {
          try {
            await api.put("/risk-management/wecom-robot", v);
            message.success("机器人配置已保存");
            load();
          } catch (e: any) {
            message.error(e.response?.data?.message || "保存失败");
          }
        }}
      >
        <Form.Item name="enabled" label="启用机器人" valuePropName="checked">
          <Switch disabled={user.role !== "ADMIN"} />
        </Form.Item>
        <Form.Item
          name="riskLevels"
          label="提醒风险等级"
          rules={[{ required: true, message: "至少选择一个风险等级" }]}
        >
          <Select
            mode="multiple"
            options={[
              ["LOW", "低风险"],
              ["MEDIUM", "中风险"],
              ["HIGH", "高风险"],
              ["CRITICAL", "严重风险"],
            ].map(([value, label]) => ({ value, label }))}
          />
        </Form.Item>
        <Card size="small" title="消息模板预览" style={{ marginBottom: 16, background: "#fafafa" }}>
          <pre style={{ margin: 0, whiteSpace: "pre-wrap", fontFamily: "inherit" }}>{`企业风控采集完成
批次号：ER202608280002
需跟进风险企业：3 家
新增风险：2 家【严重风险：1家、高风险：1家】
风险升级：1 家【高风险：1家】
销售经理分布：张三 2 家，李四 1 家`}</pre>
          <div style={{ color: "#888", marginTop: 8 }}>仅本批新增风险和风险升级企业会进入提醒；持续风险不会重复发送。实际发送时会替换为真实批次号、风险数量和销售经理统计；测试消息不代表真实采集批次通知。</div>
        </Card>
        <Form.Item name="webhook" label="Webhook 地址">
          <Input
            disabled={user.role !== "ADMIN"}
            placeholder="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=..."
          />
        </Form.Item>
        {user.role === "ADMIN" && (
          <Space>
            <Button type="primary" htmlType="submit">
              保存配置
            </Button>
            <Button
              onClick={async () => {
                try {
                  await api.post("/risk-management/wecom-robot/test");
                  message.success("测试消息已发送");
                } catch (e: any) {
                  message.error(e.response?.data?.message || "发送失败");
                }
              }}
            >
              发送测试消息
            </Button>
          </Space>
        )}
      </Form>
    </Card>
  );
}
function Accounts() {
  const [rows, setRows] = useState<any[]>([]), [roles, setRoles] = useState<any[]>([]), [open, setOpen] = useState(false), [editing, setEditing] = useState<any>(), [f] = Form.useForm();
  const load = async () => { const [a,r] = await Promise.all([api.get('/accounts'), api.get('/accounts/roles')]); setRows(a.data); setRoles(r.data); };
  useEffect(() => { void load(); }, []);
  const edit = (x?: any) => { setEditing(x); f.resetFields(); if (x) f.setFieldsValue(x); setOpen(true); };
  const save = async (v:any) => { try { if(editing) await api.put(`/accounts/${editing.id}`,v); else await api.post('/accounts',v); message.success('保存成功'); setOpen(false); load(); } catch(e:any){message.error(e.response?.data?.message||'保存失败');} };
  return <Card title="系统账号" extra={<Button type="primary" onClick={()=>edit()}>新增账号</Button>}><Table rowKey="id" dataSource={rows} columns={[{title:'登录账号',dataIndex:'loginAccount'},{title:'姓名',dataIndex:'employeeName'},{title:'手机号',dataIndex:'phone'},{title:'角色',render:(_,x)=><span>{x.roleName || x.role}</span>},{title:'状态',render:(_,x)=><Tag color={x.enabled?'green':'default'}>{x.enabled?'启用':'停用'}</Tag>},{title:'操作',render:(_,x)=><Space><Button type="link" onClick={()=>edit(x)}>编辑</Button><Button type="link" onClick={async()=>{await api.post(`/accounts/${x.id}/status`,{enabled:!x.enabled});load();}}> {x.enabled?'停用':'启用'}</Button><Button type="link" onClick={async()=>{await api.post(`/accounts/${x.id}/reset-password`);message.success('密码已重置为 123456');}}>重置密码</Button></Space>}]}/><Modal open={open} title={editing?'编辑账号':'新增账号'} footer={null} onCancel={()=>setOpen(false)}><Form form={f} layout="vertical" onFinish={save}><Form.Item name="loginAccount" label="登录账号" rules={[{required:!editing}]}><Input disabled={!!editing}/></Form.Item><Form.Item name="employeeName" label="姓名" rules={[{required:true}]}><Input/></Form.Item><Form.Item name="phone" label="手机号"><Input/></Form.Item><Form.Item name="role" label="角色" rules={[{required:true}]}><Select options={roles.map(r=>({value:r.roleCode,label:r.roleName}))}/></Form.Item>{!editing&&<Form.Item name="password" label="初始密码"><Input.Password placeholder="默认 123456"/></Form.Item>}<Button type="primary" htmlType="submit">保存</Button></Form></Modal></Card>;
}
function Roles() {
  const [rows,setRows]=useState<any[]>([]),[defs,setDefs]=useState<any[]>([]),[open,setOpen]=useState(false),[editing,setEditing]=useState<any>(),[f]=Form.useForm();
  const load=async()=>{const [r,p]=await Promise.all([api.get('/roles'),api.get('/permissions')]);setRows(r.data);setDefs(p.data.flatMap((g:any)=>g.items));}; useEffect(()=>{void load();},[]);
  const edit=(x?:any)=>{setEditing(x);f.resetFields();if(x)f.setFieldsValue({roleName:x.roleName,enabled:x.enabled,permissions:x.permissions});setOpen(true);};
  const save=async(v:any)=>{try{if(editing)await api.put(`/roles/${editing.roleCode}`,v);else await api.post('/roles',v);message.success('保存成功');setOpen(false);load();}catch(e:any){message.error(e.response?.data?.message||'保存失败');}};
  return <Card title="角色管理" extra={<Button type="primary" onClick={()=>edit()}>新增角色</Button>}><Table rowKey="roleCode" dataSource={rows} columns={[{title:'标识',dataIndex:'roleCode'},{title:'角色名称',dataIndex:'roleName'},{title:'绑定账号',dataIndex:'accountCount'},{title:'状态',render:(_,x)=><Tag color={x.enabled?'green':'default'}>{x.enabled?'启用':'停用'}</Tag>},{title:'操作',render:(_,x)=><Space><Button type="link" onClick={()=>edit(x)}>编辑权限</Button><Button type="link" onClick={async()=>{try{await api.post(`/roles/${x.roleCode}/status`,{enabled:!x.enabled});load();}catch(e:any){message.error(e.response?.data?.message||'操作失败')}}}>{x.enabled?'停用':'启用'}</Button></Space>}]}/><Modal open={open} title={editing?'编辑角色':'新增角色'} footer={null} onCancel={()=>setOpen(false)}><Form form={f} layout="vertical" onFinish={save}><Form.Item name="roleCode" label="角色标识" rules={[{required:!editing}]}><Input disabled={!!editing} placeholder="如 RISK_REVIEW"/></Form.Item><Form.Item name="roleName" label="角色名称" rules={[{required:true}]}><Input/></Form.Item><Form.Item name="enabled" label="启用" valuePropName="checked"><Switch defaultChecked/></Form.Item><Form.Item name="permissions" label="菜单及操作权限"><Select mode="multiple" options={defs.map((x:any)=>({value:x.code,label:x.name}))}/></Form.Item><Button type="primary" htmlType="submit">保存</Button></Form></Modal></Card>;
}
function Main({ user }: { user: User }) {
  const role = String(user.role || "").toUpperCase();
  const [k, setK] = useState(
    () => localStorage.getItem("er_active_menu") || "risk",
  );
  const perms = new Set(user.permissions || (role === "ADMIN" ? ["*"] : []));
  const canView = (key: string) => role === "ADMIN" || perms.has("*") || perms.has(`MENU_${key.toUpperCase()}`);
  useEffect(() => { if (!canView(k)) setK("risk"); }, [role, k]);
  const menuItems = [
    { key: "risk", label: "企业风控" },
    { key: "model", label: "风险等级" },
    { key: "key", label: "风控密钥" },
    { key: "robot", label: "群机器人" },
    ...(canView("accounts") ? [{ key: "accounts", label: "系统账号" }] : []),
    ...(canView("roles") ? [{ key: "roles", label: "角色管理" }] : []),
  ].filter((x) => canView(x.key));
  return (
    <Layout style={{ minHeight: "100vh" }}>
      <Layout.Sider>
        <div style={{ color: "#fff", padding: 20, fontSize: 18 }}>企业风控</div>
        <Menu
          theme="dark"
          selectedKeys={[k]}
          onClick={(e) => {
            setK(e.key);
            localStorage.setItem("er_active_menu", e.key);
          }}
          items={menuItems}
        />
      </Layout.Sider>
      <Layout>
        <Layout.Header style={{ background: "#fff", textAlign: "right" }}>
          {user.employeeName}（{user.roleName || roleNames[user.role] || user.role}）{" "}
          <Button
            type="link"
            onClick={() => {
              localStorage.clear();
              location.reload();
            }}
          >
            退出
          </Button>
        </Layout.Header>
        <Layout.Content style={{ padding: 24 }}>
          {k === "risk" ? (
            <Risk user={user} />
          ) : k === "model" ? (
            <Models user={user} />
          ) : k === "key" ? (
            <Keys user={user} />
          ) : k === "accounts" ? <Accounts /> : k === "roles" ? <Roles /> : (
            <Robot user={user} />
          )}
        </Layout.Content>
      </Layout>
    </Layout>
  );
}
function Root() {
  const [u, setU] = useState<User | null>(() => {
    try {
      return JSON.parse(localStorage.getItem("er_user") || "null");
    } catch {
      return null;
    }
  });
  return u ? <Main user={u} /> : <Login done={setU} />;
}
createRoot(document.getElementById("root")!).render(
  <App>
    <style>{`.ant-drawer .ant-descriptions-view table{table-layout:fixed!important}.ant-drawer .ant-descriptions-item-content{width:35%!important}`}</style>
    <Root />
  </App>,
);
