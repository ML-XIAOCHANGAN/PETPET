from __future__ import annotations

import os
import re
import sys
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from xml.sax.saxutils import escape

from docx import Document
from docx.enum.section import WD_ORIENT
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT, WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import inch
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    KeepTogether,
    ListFlowable,
    ListItem,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
)


项目根目录 = Path(__file__).resolve().parents[3]
成果目录 = 项目根目录 / "docs" / "成果"
质量检查目录 = 成果目录 / "质量检查"
文档路径 = 成果目录 / "PETPET_产品与工程综合优化报告.docx"
PDF路径 = 成果目录 / "PETPET_产品与工程综合优化报告.pdf"

中文字体 = "Microsoft YaHei"
西文字体 = "Calibri"
等宽字体 = "Consolas"

蓝色 = "2E74B5"
深蓝 = "1F4D78"
墨蓝 = "0B2545"
灰色 = "5B6573"
浅灰 = "F2F4F7"
提示底色 = "F4F6F9"
边框色 = "D9DEE7"
风险红 = "9B1C1C"
谨慎金 = "7A5A00"
白色 = "FFFFFF"
黑色 = "111418"


报告元数据 = {
    "评审日期": "2026 年 8 月 9 日",
    "评审范围": "安卓客户端、悬浮服务、本地数据、聊天与社区原型、构建与测试记录",
    "评审口径": "以当前源码和最新生成记录为主，历史说明文档为辅；未完成真机验证的事项不视为通过",
    "文档状态": "内部评审稿；用于确定从本地原型走向可内测产品的工程顺序",
}


首页快照 = [
    ["产品形态", "固定小猫形象、每日聊天与互动、三入口界面、系统悬浮陪伴、社区演示"],
    ["工程基线", "Kotlin、Jetpack Compose、安卓最低版本 26、目标版本 35、Java 17、版本 0.2.0"],
    ["当前数据", "宠物、好感度和社区主要使用 SharedPreferences；主题与日记使用 DataStore；均为设备本地数据"],
    ["验证证据", "调试安装包构建成功；静态检查为 0 错误、20 警告；英文目录联接下 12 项单元测试全部通过"],
    ["上线边界", "尚无账号、服务端权威数据、真实社区、内容审核、模型网关、真机兼容矩阵和发布审计"],
]


阅读导航 = [
    "评审口径与执行结论",
    "产品与工程现状",
    "分级优化建议",
    "安卓权限与悬浮服务风险",
    "数据治理与测试策略",
    "智能聊天安全接入方案",
    "社区治理与发布验收清单",
    "推进顺序、度量与资料来源",
]


章节 = [
    {
        "标题": "一、评审口径与执行结论",
        "分页": False,
        "内容": [
            ("小标题", "1.1 评审对象与证据等级"),
            ("正文", "本报告评审的是一个可运行的安卓本地原型，而不是已经具备线上服务能力的完整社区产品。判断顺序为：当前源码与构建产物优先，其次是最新检查报告，再次是项目说明文档。未在真机、真实账号、真实网络和真实审核后台中执行的流程，仅能视为设计或原型能力。"),
            ("项目符号", [
                "强证据：AndroidManifest.xml、app/build.gradle.kts、核心 Kotlin 源码、最新静态检查文本、最新单元测试结果和现有调试安装包。",
                "中等证据：README.md 与 docs/01 至 docs/11，能够说明需求、设计意图、历史实现、加固结果和已知边界。",
                "待验证证据：真机悬浮体验、厂商后台限制、通知可见性、锁屏与重启恢复、真实多人社区、人工智能模型安全和正式应用商店审核。",
            ]),
            ("小标题", "1.2 总体结论"),
            ("提示", "执行结论", "PETPET 已经形成清晰的陪伴型产品表达、可复用猫咪素材、主要界面和系统悬浮交互原型，并完成基础可靠性加固与可重复测试。当前仍是单设备、单进程、本地规则驱动的演示版本。下一阶段应把现有基线接入持续集成，再补齐服务端权威数据、悬浮服务真机合规、智能聊天安全和社区治理闭环。"),
            ("表格", {
                "标题": "表 1  综合评估",
                "表头": ["维度", "当前判断", "关键依据", "下一道门槛"],
                "行": [
                    ["产品定位", "已形成", "陪伴、低负担、社区分享的价值主线一致", "用真实用户验证留存与打扰边界"],
                    ["界面与素材", "原型可用", "Compose 三入口、猫咪图片与极简交互已落地", "真机、多字号、横屏和无障碍验收"],
                    ["悬浮服务", "技术原型可用", "应用上层窗口、前台服务、拖动和环形菜单已实现", "权限撤销、通知拒绝、重启和厂商后台矩阵"],
                    ["数据与账号", "未达到内测", "设备本地存储，无账号和服务端唯一约束", "服务端权威记录、迁移、幂等和删除流程"],
                    ["智能聊天", "仅本地降级", "规则式回复，无模型密钥和外部调用", "服务端模型网关、审核、额度和高风险处置"],
                    ["社区治理", "演示级", "本地动态、喜欢、评论与举报提示", "登录追责、审核后台、举报闭环和审计"],
                    ["质量与发布", "基础基线已建立", "12 项单测通过，静态检查 0 错误、20 警告，安卓 35 模拟器关键路径通过", "持续集成、真机矩阵、发布材料与监控"],
                ],
                "列宽": [1.05, 1.05, 2.2, 2.2],
            }),
        ],
    },
    {
        "标题": "二、产品与工程现状",
        "分页": True,
        "内容": [
            ("小标题", "2.1 已具备的原型能力"),
            ("项目符号", [
                "产品层：固定小猫、每日聊天与互动、好感度、成长与日记、社区动态、日夜主题和系统悬浮陪伴已经形成统一体验。",
                "界面层：Kotlin 与 Jetpack Compose 构建三入口界面，猫咪形象使用用户提供的图片资源，交互从体素堆叠收敛为极简风格。",
                "悬浮层：使用 TYPE_APPLICATION_OVERLAY 和前台服务显示小猫，支持拖动、四边吸附、环形菜单、返回应用、进入聊天和关闭。",
                "规则层：第三次有效聊天奖励 5 点、每天前五次互动各奖励 1 点的规则已提取到 AffectionRules；当前三个测试套件共 12 项测试。",
                "持久化层：设备本地保存宠物、好感度、每日进度、社区、主题和日记，适合离线原型体验。",
            ]),
            ("小标题", "2.2 源码揭示的真实边界"),
            ("表格", {
                "标题": "表 2  源码事实与产品影响",
                "表头": ["源码事实", "位置", "产品或工程影响"],
                "行": [
                    ["无宠物时会自动创建名为“小铃铛”的宠物", "PetPetViewModel.kt", "与需求中的首次领养和命名流程不一致；不能据此证明唯一领养闭环"],
                    ["宠物、好感度、社区使用 SharedPreferences", "PetPetRepository.kt", "缺少账号、并发、跨设备、事务、模式迁移和服务端权威性"],
                    ["日记以 JSON 字符串存入 DataStore", "PetPetPreferencesRepository.kt", "少量原型数据可用；扩大后查询、迁移、损坏恢复和容量控制困难"],
                    ["聊天回复来自客户端关键词规则", "PetPetViewModel.kt", "这是离线降级能力，不是智能聊天服务，也未覆盖系统化高风险分类"],
                    ["举报只显示演示提示", "PetPetViewModel.kt", "没有审核队列、处理状态、管理员、审计和申诉链路"],
                    ["发布构建未开启压缩混淆", "app/build.gradle.kts", "正式发布前需要评估体积、符号保留、崩溃解析和反编译风险"],
                    ["应用允许系统备份", "AndroidManifest.xml", "本地宠物、社区或日记可能随备份迁移；上线前需明确备份规则与隐私边界"],
                ],
                "列宽": [2.05, 1.55, 2.9],
            }),
            ("小标题", "2.3 当前构建与测试证据"),
            ("提示", "证据冲突处理", "原始中文路径直接运行单元测试仍会因测试工作进程类路径解码出现 ClassNotFoundException；从指向同一工作区的纯英文目录联接运行后，三个测试套件共 12 项全部通过。报告同时保留这两个事实，不把环境问题误报为业务失败，也不把英文路径绕行误报为原路径已修复。"),
            ("项目符号", [
                "调试安装包 app/build/outputs/apk/debug/app-debug.apk 构建成功，取证时大小为 12,972,948 字节。",
                "静态检查为 0 错误、20 警告；除依赖版本外，还包括 8 个保留但尚未使用的图片资源、圆形图标轮廓和只由代码创建的悬浮视图构造器。",
                "英文目录联接下 12 项单元测试全部通过、0 失败、0 跳过；持续集成仍应直接使用纯英文短路径。",
                "安卓 35 模拟器已覆盖首页、聊天计数、日夜主题、成长、社区、悬浮权限跳转、通知拒绝后启动及关闭清理；仍没有厂商真机兼容性证据。",
            ]),
            ("小标题", "2.4 基线发现与本轮实施边界"),
            ("正文", "本报告的基线发现以最终源码、构建记录和模拟器证据为准。本轮可靠性修改已经完成源码复核、构建、静态检查和单元测试；原始图片与 docs/references/cat-assets 保持不变。未完成厂商真机、真实账号、真实社区和模型网关验证的建议仍保持为待办。"),
        ],
    },
    {
        "标题": "三、分级优化建议",
        "分页": True,
        "内容": [
            ("小标题", "3.1 一级优先级：立即建立可信基线"),
            ("编号", [
                "把已验证的纯英文短路径构建方式固化到持续集成，依次执行 assembleDebug、testDebugUnitTest 和 lintDebug，并保存测试、静态检查和安装包信息。",
                "恢复真实首启流程。去除无宠物时自动领养的生产行为，把领养、命名、重复点击和恢复状态做成显式状态机；演示数据只能通过调试开关注入。",
                "在本轮进程内真实运行态基础上补齐启动中、停止中、权限失效和可恢复错误状态，并为系统回收建立可观测证据。",
                "继续处理发布前质量项：圆形图标、备份规则、权限撤销后的边界、依赖升级和剩余静态警告。",
            ]),
            ("小标题", "3.2 二级优先级：内测前形成端到端闭环"),
            ("编号", [
                "建立账号与服务端权威数据：身份认证、唯一领养、好感度每日结算、幂等键、数据库唯一约束和跨设备恢复。",
                "重构客户端数据层：接口、领域模型和缓存分离；DataStore 只保存轻量设置，结构化缓存迁移到 Room；制定版本迁移和损坏恢复。",
                "接入智能聊天网关：模型密钥仅在服务端，有限上下文、输入输出审核、每日额度、限流、超时、降级和安全响应必须同时上线。",
                "建立真实社区治理：登录追责、发布审核、举报、拉黑、内容下架、用户封禁、管理员权限和操作审计形成闭环。",
                "建立自动化测试矩阵：规则、仓库、视图模型、Compose 界面、权限拒绝、服务生命周期、跨日边界、重复提交和升级迁移。",
            ]),
            ("小标题", "3.3 三级优先级：发布前完成可运营化"),
            ("项目符号", [
                "完成安卓 8.0 至目标版本的代表性设备测试，并覆盖至少三个主流厂商的后台、自启动、通知和悬浮权限路径。",
                "建立崩溃、无响应、接口错误、模型成本、审核积压、举报处理时长和悬浮服务异常退出监控。",
                "完成隐私政策、服务条款、社区规范、账号注销、数据导出与删除、权限用途说明和应用商店材料。",
                "进行耗电、内存、动画帧率、安装包体积、资源清理、无障碍、字体放大、横屏和手势导航验收。",
            ]),
            ("表格", {
                "标题": "表 3  建议推进门槛",
                "表头": ["阶段", "建议周期", "核心产物", "退出条件"],
                "行": [
                    ["可信原型", "第 1 至 2 周", "稳定构建、测试报告、首启流程、悬浮状态机、权限异常处理", "构建与规则测试可重复；权限拒绝和撤销无崩溃、无残留"],
                    ["封闭内测", "第 3 至 6 周", "账号、唯一领养、服务端好感、聊天网关、社区审核、基础后台", "核心闭环使用服务端权威数据；高风险内容和举报可处置"],
                    ["候选发布", "第 7 至 10 周", "多机型回归、监控、备份恢复、隐私与商店材料、灰度方案", "无严重安全与数据问题；关键质量门槛全部有证据"],
                ],
                "列宽": [1.0, 1.0, 2.7, 1.8],
            }),
        ],
    },
    {
        "标题": "四、安卓权限与悬浮服务风险",
        "分页": True,
        "内容": [
            ("小标题", "4.1 当前声明与行为"),
            ("正文", "清单文件声明应用上层窗口、前台服务、特殊用途前台服务、通知和开机完成广播权限。悬浮服务使用 specialUse 类型，窗口使用 TYPE_APPLICATION_OVERLAY；用户第一次开启后会同步启用自动恢复，应用再次启动或设备完成启动时尝试恢复服务。服务返回 START_NOT_STICKY，通知提供停止操作。"),
            ("表格", {
                "标题": "表 4  权限与悬浮服务风险清单",
                "表头": ["风险", "当前证据", "影响", "建议与验收"],
                "行": [
                    ["特殊用途前台服务说明不足", "清单中已填写用途文本", "商店审核可能要求用途与用户价值、持续运行必要性完全一致", "发布渠道逐项核对；说明页、常驻通知、商店文案和实际行为保持一致"],
                    ["通知权限拒绝后的可见性", "拒绝通知后仍按安卓规则启动前台服务；模拟器确认通知应用操作为拒绝且服务、悬浮窗均存在", "通知不出现在通知抽屉，只保留在任务管理器，用户可能不易理解", "在授权说明中解释差异，并在安卓 13 至 15 真机验证"],
                    ["自动启动边界", "首次开启悬浮后写入 auto_start，启动和开机广播恢复", "与早期需求“不默认开机自启”存在表述差异，也增加打扰与审核风险", "增加独立开关和二次说明；默认值、关闭语义和升级迁移必须可测试"],
                    ["状态漂移", "运行态已改为服务成功挂载后发布的进程内状态流", "进程终止后状态会回到未运行，但系统回收与恢复仍缺少真机证据", "补充系统回收测试、错误态和状态迁移日志"],
                    ["权限撤销", "服务启动时检查权限并停止", "运行中撤销权限、厂商管家拦截或窗口添加失败仍需完整清理", "覆盖运行中撤销、设置页返回、异常 updateViewLayout 和 removeView"],
                    ["安全区域与误触", "窗口允许超出常规布局限制并吸附四边", "可能靠近刘海、手势边缘、状态栏或导航栏，影响系统操作", "使用 WindowInsets 与安全边距；横竖屏、分屏、显示缩放均测试"],
                    ["无障碍点击", "拖动未发生时已调用 performClick，并提供内容描述", "自动化语义已改善，但尚未完成读屏与切换控制真机操作", "使用读屏和外接控制器完成端到端验收，保持 48dp 以上目标"],
                    ["耗电与动画", "悬浮菜单含动画，重启后可长期运行", "长时间驻留可能产生电量、内存和厂商杀进程问题", "静止时停止不必要刷新；锁屏降频；记录 1 小时与 8 小时测试"],
                    ["本地备份", "应用 allowBackup 为 true", "偏好与日记可能跨设备恢复，造成账号与设备状态错配", "明确备份规则；令牌不备份；账号数据以服务端为准；验证恢复与注销"],
                ],
                "列宽": [1.25, 1.55, 1.65, 2.05],
            }),
            ("小标题", "4.2 推荐的悬浮状态机"),
            ("提示", "状态流", "已停止 → 等待应用上层权限 → 等待通知决定 → 启动中 → 运行中 → 停止中；任一步失败进入可恢复错误态。界面只订阅状态，不自行推断“已运行”。权限撤销、窗口添加失败、服务销毁和用户关闭都必须归一到“已停止”。"),
            ("小标题", "4.3 真机验收矩阵"),
            ("项目符号", [
                "系统版本：安卓 8、10、12、13、14、15；后续提高目标版本时补测对应版本。",
                "权限路径：允许、拒绝、拒绝后再允许、运行中撤销、通知拒绝、系统清理通知渠道。",
                "生命周期：首次开启、应用退后台、划掉任务、进程被回收、锁屏、省电模式、重启、升级覆盖安装和注销。",
                "窗口行为：四边吸附、横竖屏、分屏、刘海与挖孔、手势导航、字体与显示缩放、悬浮菜单展开后拖动。",
                "厂商行为：至少覆盖三类后台管理策略，并记录用户需要执行的系统设置步骤，禁止绕过用户选择。",
            ]),
        ],
    },
    {
        "标题": "五、数据治理与测试策略",
        "分页": True,
        "内容": [
            ("小标题", "5.1 数据目标架构"),
            ("正文", "正式内测应采用“服务端权威、客户端缓存、设置独立”的三层策略。宠物归属、好感度、每日计数、社区关系和审核状态由服务端决定；Room 保存可失效的结构化缓存和待同步队列；DataStore 只保存主题、引导完成状态、悬浮偏好等轻量设置。令牌使用安卓系统安全能力保存，不能进入普通偏好、日志或备份。"),
            ("表格", {
                "标题": "表 5  数据分类与控制建议",
                "表头": ["数据", "权威位置", "客户端策略", "保留与删除要点"],
                "行": [
                    ["账号凭证", "认证服务", "系统安全存储，仅保存短期访问材料", "退出立即清除；刷新凭证可撤销；禁止备份和日志输出"],
                    ["宠物与好感度", "业务数据库", "Room 缓存，只读展示服务端结果", "账号注销按规则删除或匿名化；每日结算留审计摘要"],
                    ["聊天内容", "按明确政策决定", "只保留有限会话和发送状态", "最小化保存；提供清除；日志只记错误码、耗时和脱敏标识"],
                    ["小猫日记", "由产品决定本地或云端", "若仅本地则加密并提供导出、删除；若云端需版本与冲突策略", "隐私政策写明；备份行为与用户选择一致"],
                    ["社区内容", "社区服务与对象存储", "分页缓存，审核状态不可由客户端伪造", "删除、下架、举报、申诉和法定留存分开处理"],
                    ["运行度量", "监控平台", "只发送必要、聚合、可解释指标", "不收集完整聊天、令牌、日记正文和未脱敏个人信息"],
                ],
                "列宽": [1.15, 1.35, 2.0, 2.0],
            }),
            ("小标题", "5.2 本地数据迁移步骤"),
            ("编号", [
                "定义版本化本地模式和 Clock 抽象，禁止业务直接读取 LocalDate.now 或 LocalDateTime.now，以便测试跨日、时区和设备时间篡改。",
                "将主题和悬浮偏好留在 DataStore；宠物、社区、日记等结构化数据迁移到 Room；为旧偏好数据编写一次性、可重复、可回滚的迁移。",
                "接入服务端后，首次登录上传仅允许上传的本地数据；宠物和好感度以服务端为准，冲突必须提示而不是静默覆盖。",
                "为损坏 JSON、迁移中断、存储空间不足、重复迁移和降级安装建立保护与测试；失败时保留可诊断信息但不泄露正文。",
            ]),
            ("小标题", "5.3 测试金字塔"),
            ("表格", {
                "标题": "表 6  自动化测试层次",
                "表头": ["层次", "重点范围", "必须新增的代表性用例", "建议门槛"],
                "行": [
                    ["领域单元测试", "好感、有效消息、每日边界、状态机", "零值、边界前后、已奖励、并发幂等、跨日与时区", "快速、确定、无安卓环境依赖"],
                    ["数据层测试", "SharedPreferences/DataStore 迁移、Room、接口映射", "损坏数据、重复迁移、离线队列、冲突、删除和备份恢复", "每次模式变化必须有迁移测试"],
                    ["视图模型测试", "单向状态流、输入验证、错误与重试", "首启领养、聊天失败、举报状态、主题与日记并发更新", "使用假仓库和可控协程调度"],
                    ["Compose 界面测试", "导航、输入、提示、字号和语义", "三入口切换、最大长度、键盘聚焦、深色模式、读屏标签", "关键路径在持续集成稳定运行"],
                    ["设备与系统测试", "权限、前台服务、悬浮窗、重启、厂商限制", "允许与拒绝、撤销、回收、锁屏、横屏、四边菜单", "模拟器覆盖版本，真机覆盖厂商"],
                    ["服务端与安全测试", "认证、授权、幂等、限流、审核、模型安全", "越权、重放、刷分、提示注入、上传恶意文件、后台审计", "严重安全问题为零才能进入公开测试"],
                ],
                "列宽": [1.1, 1.55, 2.55, 1.3],
            }),
            ("小标题", "5.4 持续集成质量门"),
            ("清单", [
                "在英文短路径检出并构建，记录 Java、Gradle、安卓工具包和依赖锁定信息。",
                "assembleDebug、testDebugUnitTest、lintDebug 任一失败即阻断合并；报告和安装包作为构建产物留存。",
                "发布候选还需执行签名构建、资源压缩验证、映射文件保留、依赖与许可证清单、安全扫描和安装升级测试。",
                "每次数据库或接口模式变更都要运行迁移、降级保护、备份恢复和旧版本升级测试。",
                "真机测试结果使用设备型号、系统版本、权限状态、厂商设置和复现步骤记录，不以“本机可用”代替矩阵证据。",
            ]),
        ],
    },
    {
        "标题": "六、智能聊天安全接入方案",
        "分页": True,
        "内容": [
            ("小标题", "6.1 推荐链路"),
            ("提示", "服务链路", "安卓客户端 → 业务接口与身份校验 → 频率、额度和输入检查 → 对话编排与有限历史 → 模型适配层 → 输出审核与安全策略 → 结构化响应 → 客户端展示或本地降级。客户端永远不保存模型服务密钥，也不直接调用模型供应商。"),
            ("小标题", "6.2 组件职责"),
            ("表格", {
                "标题": "表 7  智能聊天组件与安全责任",
                "表头": ["组件", "主要职责", "关键控制"],
                "行": [
                    ["安卓客户端", "输入、发送状态、有限缓存、删除入口和降级展示", "长度限制、节流、敏感状态提示；不保存密钥，不信任本地好感结算"],
                    ["业务接口", "身份、授权、会话归属、额度与幂等", "单用户与单设备速率限制、重放保护、请求标识和超时"],
                    ["输入安全", "识别违法、自伤、骚扰、未成年人、隐私和提示注入风险", "分类结果进入策略引擎；高风险不直接交给普通陪伴提示"],
                    ["对话编排", "固定猫咪性格、有限历史、上下文摘要和产品边界", "系统指令与用户文本隔离；不允许用户覆盖安全策略；首版不开启外部工具"],
                    ["模型适配", "供应商切换、超时、重试、成本和降级", "密钥轮换、区域与数据协议、熔断、预算上限和供应商故障隔离"],
                    ["输出安全", "审核回复并选择正常、改写、阻断或支持性响应", "禁止提供危险细节；避免自称真人或制造排他依赖；必要时建议联系现实支持"],
                    ["观测与审计", "记录耗时、错误、策略结果、成本和匿名会话标识", "默认不记录完整私密对话；访问分级；保留期和删除流程可审计"],
                ],
                "列宽": [1.2, 2.4, 2.9],
            }),
            ("小标题", "6.3 高风险场景处置"),
            ("项目符号", [
                "自伤或紧急危险：返回支持性、非评判性语言，鼓励联系可信的人和当地紧急资源；不得承诺保密、替代专业人员或拖延求助。",
                "违法、暴力、毒品和危险操作：阻断可执行细节，允许安全、教育和求助方向的替代信息。",
                "未成年人和性内容：采用更严格的阻断和上报策略，禁止任何性化未成年人内容。",
                "骚扰、仇恨与跟踪：不协助伤害或定位他人，提供降温、边界和举报建议。",
                "医疗、法律和财务问题：明确能力边界，提供一般信息和寻求合格专业帮助的路径，不以小猫人格制造权威感。",
                "个人信息与聊天分享：识别联系方式、精确位置、证件和账号信息；默认不进入社区，用户主动分享时先提醒并脱敏。",
            ]),
            ("小标题", "6.4 降级与好感度结算"),
            ("正文", "模型不可用时，客户端可以继续使用现有规则式回复，但必须显示“当前为离线陪伴回复”等明确状态。好感度奖励不能以客户端收到任何文本就自动增加；服务端应根据非空、长度、重复率、发送间隔、审核结果和幂等标识判定有效对话，并按业务时区结算。被安全阻断的高风险求助不应被惩罚，也不应通过重复发送刷奖励。"),
            ("小标题", "6.5 上线前评测集"),
            ("清单", [
                "人格一致性：温和、简短、猫咪陪伴语气，不自称有真实感受、身体或线下关系。",
                "安全正确率：高风险提示的阻断、支持性回应、误伤率和恢复对话能力。",
                "提示注入：诱导泄露系统指令、密钥、审核规则或要求忽略安全策略。",
                "隐私：包含姓名、电话、位置、日记和社区文本的输入是否被最小化记录。",
                "稳定性与成本：首字延迟、总耗时、超时率、降级率、单用户日成本和预算告警。",
                "好感防刷：空消息、重复字符、快速重放、并发请求、修改本地时间和多设备请求。",
            ]),
        ],
    },
    {
        "标题": "七、社区治理与发布验收清单",
        "分页": True,
        "内容": [
            ("小标题", "7.1 社区最小治理闭环"),
            ("正文", "本地发布、喜欢、评论和举报提示只能验证交互。进入真实内测前，所有社区写操作必须绑定可追责账号，服务端执行文本与图片审核、频率限制和授权校验；举报进入可分派队列，管理员处置需要原因、时间和操作前后摘要，并向用户提供必要的处理结果。"),
            ("编号", [
                "提交前：账号状态、参数、频率、文件类型、大小、图片元数据和恶意载荷检查。",
                "发布时：文本与图片审核，明确“已发布、审核中、被拒绝、可申诉”等状态，禁止客户端伪造。",
                "发布后：举报、拉黑、删除、下架、封禁和申诉在服务端生效，并保留权限分级与审计。",
                "运营侧：举报积压、处理时长、误判申诉、重复违规、模型审核成本和存储流量可监控。",
            ]),
            ("小标题", "7.2 发布验收清单"),
            ("三级标题", "产品与交互"),
            ("清单", [
                "首次打开可完成隐私说明、登录、领养和命名；重复或并发请求最终只有一只宠物。",
                "拒绝悬浮和通知权限后，应用内小猫、聊天、日记和社区仍可使用，且不反复打扰。",
                "所有关闭、注销、撤销权限和停止服务入口可发现、可操作、结果可验证。",
                "大字号、深色模式、横屏、分屏、手势导航和读屏下关键操作仍可完成。",
            ]),
            ("三级标题", "安卓与悬浮服务"),
            ("清单", [
                "前台服务类型、用途、启动时机、常驻通知和商店说明与目标渠道要求一致。",
                "悬浮窗在权限允许、拒绝、撤销、锁屏、回收、重启和升级后状态正确，无残留窗口。",
                "四边吸附和环形菜单不遮挡系统关键区域，拖动不误触，读屏可触发点击。",
                "至少三类厂商系统完成记录化真机测试，并给出用户可理解的设置指引。",
            ]),
            ("三级标题", "数据、安全与人工智能"),
            ("清单", [
                "令牌、模型、短信和对象存储密钥只存在服务端或系统安全存储，不进入日志、普通偏好和备份。",
                "宠物、好感度、点赞和社区关系由服务端权威结算，接口具备授权、幂等、限流和重放保护。",
                "聊天输入输出审核、高风险支持性响应、模型超时降级、额度和成本告警均通过评测。",
                "账号注销、聊天清除、日记删除、社区删除与法定留存边界在产品和隐私政策中一致。",
            ]),
            ("三级标题", "质量、运维与发布"),
            ("清单", [
                "持续集成中的构建、单元测试、静态检查、界面测试和迁移测试全部有可下载报告。",
                "不存在可稳定复现的崩溃、严重越权、数据重复、悬浮残留或审核绕过。",
                "崩溃、无响应、接口错误、模型成本、审核积压和备份失败具有监控、告警和负责人。",
                "签名、版本号、混淆映射、开源许可证、隐私清单、截图、商店文案、灰度和回滚方案齐备。",
            ]),
        ],
    },
    {
        "标题": "八、推进顺序与度量",
        "分页": True,
        "内容": [
            ("小标题", "8.1 推荐执行顺序"),
            ("表格", {
                "标题": "表 8  工作包、依赖与完成定义",
                "表头": ["工作包", "前置依赖", "完成定义", "不应混淆为完成的事项"],
                "行": [
                    ["构建与测试基线", "无", "英文路径持续集成稳定运行，报告和安装包可追溯", "仅有测试源码、仅能编译或历史报告通过"],
                    ["首启与悬浮状态", "构建基线", "显式领养、真实服务状态、权限异常和关闭路径有自动化与真机证据", "界面按钮变化或偏好值被写入"],
                    ["账号与权威数据", "接口与数据模型评审", "唯一领养、好感、删除和跨设备恢复由服务端保证", "本地 SharedPreferences 中看起来只有一只宠物"],
                    ["智能聊天", "账号、额度与安全策略", "网关、审核、降级、评测和成本监控共同上线", "本地关键词回复或单次模型演示"],
                    ["真实社区", "账号、审核和管理后台", "发布到处置全链路可审计，用户可举报和申诉", "本地动态、举报提示或静态后台页面"],
                    ["发布候选", "前述闭环、真机矩阵", "隐私、监控、备份恢复、灰度和商店审计有证据", "assembleDebug 成功或存在可安装调试包"],
                ],
                "列宽": [1.25, 1.45, 2.3, 1.5],
            }),
            ("小标题", "8.2 建议度量"),
            ("项目符号", [
                "质量：无崩溃会话占比、应用无响应率、启动耗时、静态检查阻断项、测试不稳定率和升级失败率。",
                "悬浮：授权转化、成功启动、异常停止、用户主动关闭、权限撤销清理成功率、每小时耗电与内存。",
                "陪伴：有效聊天完成率、降级率、安全阻断率、重复发送率、好感结算幂等冲突和单用户日成本。",
                "社区：审核通过与复核比例、举报率、处理时长、申诉改判率、重复违规率、图片存储与流量。",
                "隐私与运维：注销完成时长、数据删除失败、备份恢复演练、密钥轮换、告警响应和审计缺口。",
            ]),
            ("提示", "度量原则", "发布门槛应使用经过内测验证的阈值，而不是为了好看预先虚构数字。每个指标都要绑定数据口径、采样范围、告警阈值、负责人和处置动作。"),
        ],
    },
    {
        "标题": "九、资料来源与限制说明",
        "分页": True,
        "内容": [
            ("小标题", "9.1 项目内资料"),
            ("来源", [
                "README.md：产品简介、当前功能、技术基线、权限说明和正式上线边界。",
                "docs/01_需求分析.md：产品范围、功能需求、数据对象、安全隐私、验收条件和官方资料入口。",
                "docs/02_开发计划.md：端到端技术方案、服务端与后台、测试计划、风险、八周排期和质量门槛。",
                "docs/03_体素按钮视觉规范.md：视觉来源、颜色、按钮结构和代码原生实现决定。",
                "docs/04_首版实现进度.md：历史实现与历史验证记录；与最新构建记录冲突时不作为最终通过证据。",
                "docs/05_0.2.0_架构与实现说明.md：图片接入原则、代码结构、依赖和悬浮窗规则。",
                "docs/06_主页单屏与悬浮菜单改版说明.md、docs/07_极简界面与自适应环形菜单改版.md：最新界面与悬浮菜单设计、已知真机验证缺口。",
                "app/build.gradle.kts、app/src/main/AndroidManifest.xml：版本、系统基线、依赖、权限、前台服务和备份设置。",
                "PetPetViewModel.kt、PetPetRepository.kt、PetPetPreferencesRepository.kt、AffectionRules.kt：首启、本地数据、聊天、好感度和日记实现。",
                "FloatingPetController.kt、FloatingPetService.kt、FloatingPetView.kt、BootCompletedReceiver.kt：权限申请、自动恢复、窗口、通知、拖动与菜单实现。",
                "docs/08_可靠性加固说明.md、docs/11_本轮验证与技能应用记录.md：最终改动、构建、测试和模拟器证据。",
                "app/build/reports/lint-results-debug.txt：取证时静态检查为 0 错误、20 警告。",
                "app/build/test-results/testDebugUnitTest 下三个测试套件 XML：英文目录联接运行时共 12 项测试全部通过。",
            ]),
            ("小标题", "9.2 项目文档引用的外部入口"),
            ("来源", [
                "安卓开发者文档：TYPE_APPLICATION_OVERLAY 窗口类型，https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY",
                "安卓开发者文档：ACTION_MANAGE_OVERLAY_PERMISSION，https://developer.android.com/reference/android/provider/Settings#ACTION_MANAGE_OVERLAY_PERMISSION",
                "安卓开发者文档：前台服务，https://developer.android.com/develop/background-work/services/fgs",
                "安卓开发者文档：应用架构建议，https://developer.android.com/topic/architecture",
                "谷歌应用商店开发者政策：用户生成内容，https://support.google.com/googleplay/android-developer/answer/9876937",
            ]),
            ("小标题", "9.3 限制说明"),
            ("项目符号", [
                "本报告控制了安卓 35 模拟器并验证关键路径，但不能替代悬浮窗、通知、耗电、厂商后台和无障碍真机报告。",
                "本报告没有接入真实账号、服务端、社区审核后台或模型供应商，因此相关章节是安全与工程方案，不是已完成能力。",
                "外部政策入口来自项目现有文档；正式发布前必须按目标渠道、目标版本和提交日期重新核对最新要求。",
            ]),
        ],
    },
]


def 设置运行字体(运行, 字号=None, 颜色=None, 加粗=None, 斜体=None, 西文=西文字体, 中文=中文字体):
    运行.font.name = 西文
    属性 = 运行._element.get_or_add_rPr()
    字体节点 = 属性.rFonts
    if 字体节点 is None:
        字体节点 = OxmlElement("w:rFonts")
        属性.insert(0, 字体节点)
    字体节点.set(qn("w:ascii"), 西文)
    字体节点.set(qn("w:hAnsi"), 西文)
    字体节点.set(qn("w:eastAsia"), 中文)
    if 字号 is not None:
        运行.font.size = Pt(字号)
    if 颜色 is not None:
        运行.font.color.rgb = RGBColor.from_string(颜色)
    if 加粗 is not None:
        运行.bold = 加粗
    if 斜体 is not None:
        运行.italic = 斜体


def 设置样式字体(样式, 西文=西文字体, 中文=中文字体, 字号=11, 颜色=黑色, 加粗=False):
    样式.font.name = 西文
    样式.font.size = Pt(字号)
    样式.font.color.rgb = RGBColor.from_string(颜色)
    样式.font.bold = 加粗
    属性 = 样式.element.get_or_add_rPr()
    字体节点 = 属性.rFonts
    if 字体节点 is None:
        字体节点 = OxmlElement("w:rFonts")
        属性.insert(0, 字体节点)
    字体节点.set(qn("w:ascii"), 西文)
    字体节点.set(qn("w:hAnsi"), 西文)
    字体节点.set(qn("w:eastAsia"), 中文)


def 设置段落精确行距(段落, 倍数: float):
    段落.paragraph_format.line_spacing = 倍数
    间距 = 段落._p.get_or_add_pPr().get_or_add_spacing()
    间距.set(qn("w:line"), str(round(240 * 倍数)))
    间距.set(qn("w:lineRule"), "auto")


def 设置段落底纹(段落, 填充: str):
    属性 = 段落._p.get_or_add_pPr()
    底纹 = 属性.find(qn("w:shd"))
    if 底纹 is None:
        底纹 = OxmlElement("w:shd")
        属性.append(底纹)
    底纹.set(qn("w:fill"), 填充)


def 设置段落左边框(段落, 颜色: str, 宽度: int = 20):
    属性 = 段落._p.get_or_add_pPr()
    边框组 = 属性.find(qn("w:pBdr"))
    if 边框组 is None:
        边框组 = OxmlElement("w:pBdr")
        属性.append(边框组)
    左边框 = OxmlElement("w:left")
    左边框.set(qn("w:val"), "single")
    左边框.set(qn("w:sz"), str(宽度))
    左边框.set(qn("w:space"), "8")
    左边框.set(qn("w:color"), 颜色)
    边框组.append(左边框)


def 设置段落下边框(段落, 颜色: str, 宽度: int = 10):
    属性 = 段落._p.get_or_add_pPr()
    边框组 = 属性.find(qn("w:pBdr"))
    if 边框组 is None:
        边框组 = OxmlElement("w:pBdr")
        属性.append(边框组)
    下边框 = OxmlElement("w:bottom")
    下边框.set(qn("w:val"), "single")
    下边框.set(qn("w:sz"), str(宽度))
    下边框.set(qn("w:space"), "8")
    下边框.set(qn("w:color"), 颜色)
    边框组.append(下边框)


def 添加域(段落, 指令: str):
    开始 = OxmlElement("w:fldChar")
    开始.set(qn("w:fldCharType"), "begin")
    文本 = OxmlElement("w:instrText")
    文本.set(qn("xml:space"), "preserve")
    文本.text = 指令
    分隔 = OxmlElement("w:fldChar")
    分隔.set(qn("w:fldCharType"), "separate")
    显示 = OxmlElement("w:t")
    显示.text = "1"
    结束 = OxmlElement("w:fldChar")
    结束.set(qn("w:fldCharType"), "end")
    运行 = 段落.add_run()
    运行._r.extend([开始, 文本, 分隔, 显示, 结束])
    设置运行字体(运行, 字号=9, 颜色=灰色)


def 创建编号定义(文档: Document, 类型: str, 标记: str) -> int:
    编号部件 = 文档.part.numbering_part.element
    抽象编号们 = 编号部件.findall(qn("w:abstractNum"))
    抽象编号 = max([int(x.get(qn("w:abstractNumId"))) for x in 抽象编号们] + [-1]) + 1
    实例们 = 编号部件.findall(qn("w:num"))
    实例编号 = max([int(x.get(qn("w:numId"))) for x in 实例们] + [0]) + 1

    抽象 = OxmlElement("w:abstractNum")
    抽象.set(qn("w:abstractNumId"), str(抽象编号))
    唯一值 = f"{(抽象编号 + 1) * 2654435761 & 0xFFFFFFFF:08X}"
    唯一标识 = OxmlElement("w:nsid")
    唯一标识.set(qn("w:val"), 唯一值)
    抽象.append(唯一标识)
    多级 = OxmlElement("w:multiLevelType")
    多级.set(qn("w:val"), "singleLevel")
    抽象.append(多级)
    模板标识 = OxmlElement("w:tmpl")
    模板标识.set(qn("w:val"), 唯一值)
    抽象.append(模板标识)
    层 = OxmlElement("w:lvl")
    层.set(qn("w:ilvl"), "0")
    起始 = OxmlElement("w:start")
    起始.set(qn("w:val"), "1")
    层.append(起始)
    格式 = OxmlElement("w:numFmt")
    格式.set(qn("w:val"), 类型)
    层.append(格式)
    层文本 = OxmlElement("w:lvlText")
    层文本.set(qn("w:val"), 标记)
    层.append(层文本)
    对齐 = OxmlElement("w:lvlJc")
    对齐.set(qn("w:val"), "left")
    层.append(对齐)
    段落属性 = OxmlElement("w:pPr")
    制表组 = OxmlElement("w:tabs")
    制表 = OxmlElement("w:tab")
    制表.set(qn("w:val"), "num")
    制表.set(qn("w:pos"), "720")
    制表组.append(制表)
    段落属性.append(制表组)
    缩进 = OxmlElement("w:ind")
    缩进.set(qn("w:left"), "720")
    缩进.set(qn("w:hanging"), "360")
    段落属性.append(缩进)
    间距 = OxmlElement("w:spacing")
    间距.set(qn("w:after"), "160")
    间距.set(qn("w:line"), "280")
    间距.set(qn("w:lineRule"), "auto")
    段落属性.append(间距)
    层.append(段落属性)
    运行属性 = OxmlElement("w:rPr")
    字体 = OxmlElement("w:rFonts")
    字体.set(qn("w:ascii"), 中文字体 if 类型 == "bullet" else 西文字体)
    字体.set(qn("w:hAnsi"), 中文字体 if 类型 == "bullet" else 西文字体)
    字体.set(qn("w:eastAsia"), 中文字体)
    运行属性.append(字体)
    层.append(运行属性)
    抽象.append(层)
    第一个实例 = 编号部件.find(qn("w:num"))
    if 第一个实例 is None:
        编号部件.append(抽象)
    else:
        第一个实例.addprevious(抽象)

    实例 = OxmlElement("w:num")
    实例.set(qn("w:numId"), str(实例编号))
    引用 = OxmlElement("w:abstractNumId")
    引用.set(qn("w:val"), str(抽象编号))
    实例.append(引用)
    编号部件.append(实例)
    return 实例编号


def 应用编号(段落, 编号: int):
    属性 = 段落._p.get_or_add_pPr()
    编号属性 = 属性.find(qn("w:numPr"))
    if 编号属性 is None:
        编号属性 = OxmlElement("w:numPr")
        属性.append(编号属性)
    层 = OxmlElement("w:ilvl")
    层.set(qn("w:val"), "0")
    编号节点 = OxmlElement("w:numId")
    编号节点.set(qn("w:val"), str(编号))
    编号属性.extend([层, 编号节点])


def 设置表格几何(表格, 列宽英寸: list[float]):
    列宽 = [round(x * 1440) for x in 列宽英寸]
    if sum(列宽) != 9360:
        列宽[-1] += 9360 - sum(列宽)
    表格.alignment = WD_TABLE_ALIGNMENT.LEFT
    表格.autofit = False
    表格属性 = 表格._tbl.tblPr
    宽度 = 表格属性.find(qn("w:tblW"))
    if 宽度 is None:
        宽度 = OxmlElement("w:tblW")
        表格属性.append(宽度)
    宽度.set(qn("w:w"), "9360")
    宽度.set(qn("w:type"), "dxa")
    缩进 = 表格属性.find(qn("w:tblInd"))
    if 缩进 is None:
        缩进 = OxmlElement("w:tblInd")
        表格属性.append(缩进)
    缩进.set(qn("w:w"), "120")
    缩进.set(qn("w:type"), "dxa")
    布局 = 表格属性.find(qn("w:tblLayout"))
    if 布局 is None:
        布局 = OxmlElement("w:tblLayout")
        表格属性.append(布局)
    布局.set(qn("w:type"), "fixed")
    边距 = 表格属性.find(qn("w:tblCellMar"))
    if 边距 is None:
        边距 = OxmlElement("w:tblCellMar")
        表格属性.append(边距)
    for 名称, 数值 in (("top", 80), ("bottom", 80), ("start", 120), ("end", 120)):
        节点 = 边距.find(qn(f"w:{名称}"))
        if 节点 is None:
            节点 = OxmlElement(f"w:{名称}")
            边距.append(节点)
        节点.set(qn("w:w"), str(数值))
        节点.set(qn("w:type"), "dxa")
    边框组 = 表格属性.find(qn("w:tblBorders"))
    if 边框组 is None:
        边框组 = OxmlElement("w:tblBorders")
        表格属性.append(边框组)
    for 名称 in ("top", "left", "bottom", "right", "insideH", "insideV"):
        节点 = OxmlElement(f"w:{名称}")
        节点.set(qn("w:val"), "single")
        节点.set(qn("w:sz"), "4")
        节点.set(qn("w:space"), "0")
        节点.set(qn("w:color"), 边框色)
        边框组.append(节点)
    网格 = 表格._tbl.tblGrid
    for 节点 in list(网格):
        网格.remove(节点)
    for 数值 in 列宽:
        列 = OxmlElement("w:gridCol")
        列.set(qn("w:w"), str(数值))
        网格.append(列)
    for 行 in 表格.rows:
        行属性 = 行._tr.get_or_add_trPr()
        if 行属性.find(qn("w:cantSplit")) is None:
            行属性.append(OxmlElement("w:cantSplit"))
        for 单元格, 数值 in zip(行.cells, 列宽):
            单元格.width = Inches(数值 / 1440)
            单元格.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            单元格属性 = 单元格._tc.get_or_add_tcPr()
            单元格宽度 = 单元格属性.find(qn("w:tcW"))
            if 单元格宽度 is None:
                单元格宽度 = OxmlElement("w:tcW")
                单元格属性.append(单元格宽度)
            单元格宽度.set(qn("w:w"), str(数值))
            单元格宽度.set(qn("w:type"), "dxa")


def 设置表头重复(行):
    属性 = 行._tr.get_or_add_trPr()
    重复 = OxmlElement("w:tblHeader")
    重复.set(qn("w:val"), "true")
    属性.append(重复)


def 设置单元格底纹(单元格, 填充: str):
    属性 = 单元格._tc.get_or_add_tcPr()
    底纹 = 属性.find(qn("w:shd"))
    if 底纹 is None:
        底纹 = OxmlElement("w:shd")
        属性.append(底纹)
    底纹.set(qn("w:fill"), 填充)


def 添加文档表格(文档: Document, 数据: dict[str, Any]):
    标题段 = 文档.add_paragraph()
    标题段.paragraph_format.space_before = Pt(4)
    标题段.paragraph_format.space_after = Pt(4)
    标题段.paragraph_format.keep_with_next = True
    设置段落精确行距(标题段, 1.0)
    设置运行字体(标题段.add_run(数据["标题"]), 字号=9, 颜色=灰色, 加粗=True)
    表格 = 文档.add_table(rows=1, cols=len(数据["表头"]))
    表格.style = "Table Grid"
    for 索引, 文本 in enumerate(数据["表头"]):
        单元格 = 表格.rows[0].cells[索引]
        设置单元格底纹(单元格, 浅灰)
        段落 = 单元格.paragraphs[0]
        段落.alignment = WD_ALIGN_PARAGRAPH.CENTER
        段落.paragraph_format.space_before = Pt(0)
        段落.paragraph_format.space_after = Pt(0)
        设置段落精确行距(段落, 1.1)
        设置运行字体(段落.add_run(文本), 字号=9, 颜色=墨蓝, 加粗=True)
    设置表头重复(表格.rows[0])
    for 行数据 in 数据["行"]:
        行 = 表格.add_row()
        for 索引, 文本 in enumerate(行数据):
            单元格 = 行.cells[索引]
            段落 = 单元格.paragraphs[0]
            段落.paragraph_format.space_before = Pt(0)
            段落.paragraph_format.space_after = Pt(0)
            设置段落精确行距(段落, 1.15)
            对齐 = WD_ALIGN_PARAGRAPH.CENTER if len(str(文本)) <= 10 and 索引 < 2 else WD_ALIGN_PARAGRAPH.LEFT
            段落.alignment = 对齐
            设置运行字体(段落.add_run(str(文本)), 字号=9, 颜色=黑色, 加粗=(索引 == 0))
    设置表格几何(表格, 数据["列宽"])
    文档.add_paragraph().paragraph_format.space_after = Pt(2)


def 配置文档样式(文档: Document):
    节 = 文档.sections[0]
    节.orientation = WD_ORIENT.PORTRAIT
    节.page_width = Inches(8.5)
    节.page_height = Inches(11)
    节.top_margin = Inches(1)
    节.bottom_margin = Inches(1)
    节.left_margin = Inches(1)
    节.right_margin = Inches(1)
    节.header_distance = Inches(0.492)
    节.footer_distance = Inches(0.492)
    节.different_first_page_header_footer = False

    普通 = 文档.styles["Normal"]
    设置样式字体(普通, 字号=11, 颜色=黑色)
    普通.paragraph_format.space_before = Pt(0)
    普通.paragraph_format.space_after = Pt(6)
    普通.paragraph_format.line_spacing = 1.1
    普通.paragraph_format.widow_control = True

    标题 = 文档.styles["Title"]
    设置样式字体(标题, 字号=24, 颜色=黑色, 加粗=True)
    标题.paragraph_format.space_before = Pt(0)
    标题.paragraph_format.space_after = Pt(4)
    标题.paragraph_format.line_spacing = 1.0

    副标题 = 文档.styles["Subtitle"]
    设置样式字体(副标题, 字号=13, 颜色=灰色)
    副标题.paragraph_format.space_before = Pt(0)
    副标题.paragraph_format.space_after = Pt(14)
    副标题.paragraph_format.line_spacing = 1.1

    for 名称, 字号, 颜色, 前, 后 in (
        ("Heading 1", 16, 蓝色, 16, 8),
        ("Heading 2", 13, 蓝色, 12, 6),
        ("Heading 3", 12, 深蓝, 8, 4),
    ):
        样式 = 文档.styles[名称]
        设置样式字体(样式, 字号=字号, 颜色=颜色, 加粗=True)
        样式.paragraph_format.space_before = Pt(前)
        样式.paragraph_format.space_after = Pt(后)
        样式.paragraph_format.line_spacing = 1.0
        样式.paragraph_format.keep_with_next = True
        样式.paragraph_format.keep_together = True

    页眉 = 节.header.paragraphs[0]
    页眉.text = ""
    页眉.paragraph_format.space_before = Pt(0)
    页眉.paragraph_format.space_after = Pt(0)
    页眉.paragraph_format.tab_stops.add_tab_stop(Inches(6.5))
    设置运行字体(页眉.add_run("PETPET｜产品与工程综合优化报告"), 字号=8.5, 颜色=灰色, 加粗=True)
    页眉.add_run("\t")
    设置运行字体(页眉.add_run("内部评审"), 字号=8.5, 颜色=灰色)

    页脚 = 节.footer.paragraphs[0]
    页脚.text = ""
    页脚.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    页脚.paragraph_format.space_before = Pt(0)
    页脚.paragraph_format.space_after = Pt(0)
    设置运行字体(页脚.add_run("第 "), 字号=8.5, 颜色=灰色)
    添加域(页脚, "PAGE")
    设置运行字体(页脚.add_run(" 页 / 共 "), 字号=8.5, 颜色=灰色)
    添加域(页脚, "NUMPAGES")
    设置运行字体(页脚.add_run(" 页"), 字号=8.5, 颜色=灰色)


def 添加首页(文档: Document, 编号数字: int):
    空白 = 文档.add_paragraph()
    空白.paragraph_format.space_after = Pt(10)
    引导 = 文档.add_paragraph()
    引导.paragraph_format.space_after = Pt(3)
    设置运行字体(引导.add_run("内部评审｜安卓原型到内测产品"), 字号=10, 颜色=蓝色, 加粗=True)
    标题 = 文档.add_paragraph(style="Title")
    标题.add_run("PETPET 产品与工程综合优化报告")
    副标题 = 文档.add_paragraph(style="Subtitle")
    副标题.add_run("从本地原型走向可内测产品的优先级、风险与验收基线")
    for 标签, 文本 in 报告元数据.items():
        段落 = 文档.add_paragraph()
        段落.paragraph_format.space_before = Pt(0)
        段落.paragraph_format.space_after = Pt(2)
        设置段落精确行距(段落, 1.05)
        设置运行字体(段落.add_run(f"{标签}："), 字号=10, 颜色=黑色, 加粗=True)
        设置运行字体(段落.add_run(文本), 字号=10, 颜色=灰色)
    设置段落下边框(文档.paragraphs[-1], 蓝色, 8)

    提示 = 文档.add_paragraph()
    提示.paragraph_format.space_before = Pt(14)
    提示.paragraph_format.space_after = Pt(12)
    提示.paragraph_format.left_indent = Pt(10)
    提示.paragraph_format.right_indent = Pt(6)
    设置段落精确行距(提示, 1.15)
    设置段落底纹(提示, 提示底色)
    设置段落左边框(提示, 蓝色, 20)
    设置运行字体(提示.add_run("核心判断："), 字号=11, 颜色=墨蓝, 加粗=True)
    设置运行字体(提示.add_run("视觉与核心交互原型已经形成，但当前不具备正式上线条件。应先修复可复现构建与测试，再完成服务端权威数据、合规悬浮、智能聊天安全和社区治理闭环。"), 字号=11, 颜色=黑色)

    添加文档表格(文档, {
        "标题": "项目快照",
        "表头": ["项目", "取证结论"],
        "行": 首页快照,
        "列宽": [1.25, 5.25],
    })
    文档.add_page_break()
    导航标题 = 文档.add_paragraph(style="Heading 2")
    导航标题.add_run("阅读导航")
    for 项 in 阅读导航:
        段落 = 文档.add_paragraph()
        应用编号(段落, 编号数字)
        设置运行字体(段落.add_run(项), 字号=10.5, 颜色=黑色)


def 添加内容块(文档: Document, 类型: str, 数据: Any, 编号们: dict[str, int]):
    if 类型 == "正文":
        段落 = 文档.add_paragraph()
        设置运行字体(段落.add_run(数据), 字号=11, 颜色=黑色)
    elif 类型 == "小标题":
        段落 = 文档.add_paragraph(style="Heading 2")
        段落.add_run(数据)
    elif 类型 == "三级标题":
        段落 = 文档.add_paragraph(style="Heading 3")
        段落.add_run(数据)
    elif 类型 == "提示":
        标签, 文本 = 数据, None
        if isinstance(数据, tuple):
            标签, 文本 = 数据
        else:
            raise ValueError("提示内容必须是标签与正文")
        段落 = 文档.add_paragraph()
        段落.paragraph_format.space_before = Pt(5)
        段落.paragraph_format.space_after = Pt(8)
        段落.paragraph_format.left_indent = Pt(10)
        段落.paragraph_format.right_indent = Pt(6)
        设置段落精确行距(段落, 1.15)
        设置段落底纹(段落, 提示底色)
        设置段落左边框(段落, 蓝色, 20)
        设置运行字体(段落.add_run(f"{标签}："), 字号=10.5, 颜色=墨蓝, 加粗=True)
        设置运行字体(段落.add_run(文本), 字号=10.5, 颜色=黑色)
    elif 类型 in ("项目符号", "编号", "清单", "来源"):
        编号键 = {"项目符号": "项目符号", "编号": "编号", "清单": "清单", "来源": "编号"}[类型]
        字号 = 10 if 类型 == "来源" else 10.5
        颜色 = 灰色 if 类型 == "来源" else 黑色
        for 项 in 数据:
            段落 = 文档.add_paragraph()
            应用编号(段落, 编号们[编号键])
            设置运行字体(段落.add_run(项), 字号=字号, 颜色=颜色)
    elif 类型 == "表格":
        添加文档表格(文档, 数据)
    else:
        raise ValueError(f"未知内容块：{类型}")


def 生成文档():
    文档 = Document()
    文档.core_properties.title = "PETPET 产品与工程综合优化报告"
    文档.core_properties.subject = "PETPET 安卓应用产品、工程、安全、测试与发布综合评审"
    文档.core_properties.author = "PETPET 项目评审"
    文档.core_properties.keywords = "PETPET, 安卓, 悬浮服务, 数据治理, 测试, 智能聊天, 社区治理"
    配置文档样式(文档)
    编号们 = {
        "项目符号": 创建编号定义(文档, "bullet", "•"),
        "编号": 创建编号定义(文档, "decimal", "%1."),
        "清单": 创建编号定义(文档, "bullet", "□"),
    }
    添加首页(文档, 编号们["编号"])
    for 章节数据 in 章节:
        标题 = 文档.add_paragraph(style="Heading 1")
        标题.add_run(章节数据["标题"])
        for 项 in 章节数据["内容"]:
            if 项[0] == "提示":
                添加内容块(文档, "提示", (项[1], 项[2]), 编号们)
            else:
                添加内容块(文档, 项[0], 项[1], 编号们)
    更新域 = 文档.settings.element.find(qn("w:updateFields"))
    if 更新域 is None:
        更新域 = OxmlElement("w:updateFields")
        文档.settings.element.append(更新域)
    更新域.set(qn("w:val"), "true")
    成果目录.mkdir(parents=True, exist_ok=True)
    文档.save(文档路径)
    return 文档路径


def 转义段落文本(文本: str) -> str:
    return escape(文本).replace("\n", "<br/>")


def 注册PDF字体():
    常规 = Path("C:/Windows/Fonts/msyh.ttc")
    粗体 = Path("C:/Windows/Fonts/msyhbd.ttc")
    if not 常规.exists() or not 粗体.exists():
        raise FileNotFoundError("未找到 Microsoft YaHei 字体文件")
    pdfmetrics.registerFont(TTFont("微软雅黑", str(常规), subfontIndex=0))
    pdfmetrics.registerFont(TTFont("微软雅黑粗体", str(粗体), subfontIndex=0))
    pdfmetrics.registerFontFamily("微软雅黑", normal="微软雅黑", bold="微软雅黑粗体")


def PDF样式():
    基础 = getSampleStyleSheet()
    return {
        "正文": ParagraphStyle("正文", parent=基础["BodyText"], fontName="微软雅黑", fontSize=10.2, leading=15.2, textColor=colors.HexColor("#111418"), spaceAfter=6, wordWrap="CJK"),
        "标题": ParagraphStyle("标题", parent=基础["Title"], fontName="微软雅黑粗体", fontSize=24, leading=31, textColor=colors.HexColor("#111418"), alignment=TA_LEFT, spaceAfter=5, wordWrap="CJK"),
        "副标题": ParagraphStyle("副标题", parent=基础["BodyText"], fontName="微软雅黑", fontSize=12.5, leading=18, textColor=colors.HexColor("#5B6573"), spaceAfter=13, wordWrap="CJK"),
        "一级": ParagraphStyle("一级", parent=基础["Heading1"], fontName="微软雅黑粗体", fontSize=15, leading=21, textColor=colors.HexColor("#2E74B5"), spaceBefore=13, spaceAfter=7, keepWithNext=True, wordWrap="CJK"),
        "二级": ParagraphStyle("二级", parent=基础["Heading2"], fontName="微软雅黑粗体", fontSize=12.5, leading=18, textColor=colors.HexColor("#2E74B5"), spaceBefore=10, spaceAfter=5, keepWithNext=True, wordWrap="CJK"),
        "三级": ParagraphStyle("三级", parent=基础["Heading3"], fontName="微软雅黑粗体", fontSize=11, leading=16, textColor=colors.HexColor("#1F4D78"), spaceBefore=7, spaceAfter=4, keepWithNext=True, wordWrap="CJK"),
        "表格": ParagraphStyle("表格", parent=基础["BodyText"], fontName="微软雅黑", fontSize=8.2, leading=11.8, textColor=colors.HexColor("#111418"), spaceAfter=0, wordWrap="CJK"),
        "表头": ParagraphStyle("表头", parent=基础["BodyText"], fontName="微软雅黑粗体", fontSize=8.2, leading=11.5, textColor=colors.HexColor("#0B2545"), alignment=TA_CENTER, spaceAfter=0, wordWrap="CJK"),
        "表题": ParagraphStyle("表题", parent=基础["BodyText"], fontName="微软雅黑粗体", fontSize=8.3, leading=11, textColor=colors.HexColor("#5B6573"), spaceBefore=4, spaceAfter=4, keepWithNext=True, wordWrap="CJK"),
        "列表": ParagraphStyle("列表", parent=基础["BodyText"], fontName="微软雅黑", fontSize=9.8, leading=14.5, leftIndent=0, firstLineIndent=0, spaceAfter=2, wordWrap="CJK"),
        "来源": ParagraphStyle("来源", parent=基础["BodyText"], fontName="微软雅黑", fontSize=8.7, leading=13, textColor=colors.HexColor("#5B6573"), spaceAfter=2, wordWrap="CJK"),
        "提示": ParagraphStyle("提示", parent=基础["BodyText"], fontName="微软雅黑", fontSize=9.8, leading=14.7, textColor=colors.HexColor("#111418"), backColor=colors.HexColor("#F4F6F9"), borderColor=colors.HexColor("#2E74B5"), borderWidth=0, borderPadding=(7, 8, 7, 11), leftIndent=0, rightIndent=0, spaceBefore=5, spaceAfter=8, wordWrap="CJK"),
        "元数据": ParagraphStyle("元数据", parent=基础["BodyText"], fontName="微软雅黑", fontSize=9.2, leading=13, textColor=colors.HexColor("#5B6573"), spaceAfter=2, wordWrap="CJK"),
    }


def PDF页眉页脚(画布, 文档):
    画布.saveState()
    画布.setStrokeColor(colors.HexColor("#D9DEE7"))
    画布.setLineWidth(0.45)
    画布.line(72, letter[1] - 44, letter[0] - 72, letter[1] - 44)
    画布.setFont("微软雅黑", 7.8)
    画布.setFillColor(colors.HexColor("#5B6573"))
    画布.drawString(72, letter[1] - 35, "PETPET｜产品与工程综合优化报告")
    画布.drawRightString(letter[0] - 72, letter[1] - 35, "内部评审")
    画布.line(72, 43, letter[0] - 72, 43)
    画布.drawRightString(letter[0] - 72, 31, f"第 {文档.page} 页")
    画布.drawString(72, 31, "2026 年 8 月 9 日")
    画布.restoreState()


def PDF表格(数据: dict[str, Any], 样式们):
    流 = [Paragraph(转义段落文本(数据["标题"]), 样式们["表题"])]
    表数据 = [[Paragraph(转义段落文本(x), 样式们["表头"]) for x in 数据["表头"]]]
    for 行 in 数据["行"]:
        表数据.append([Paragraph(转义段落文本(str(x)), 样式们["表格"]) for x in 行])
    列宽 = [x * inch for x in 数据["列宽"]]
    表 = Table(表数据, colWidths=列宽, repeatRows=1, hAlign="LEFT", splitByRow=True)
    表.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#F2F4F7")),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.HexColor("#0B2545")),
        ("GRID", (0, 0), (-1, -1), 0.45, colors.HexColor("#D9DEE7")),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]))
    流.extend([表, Spacer(1, 7)])
    return 流


def PDF列表(项目: list[str], 样式, 类型: str):
    条目 = [ListItem(Paragraph(转义段落文本(x), 样式), leftIndent=0) for x in 项目]
    if 类型 == "编号":
        return ListFlowable(条目, bulletType="1", start="1", leftIndent=24, bulletFontName="微软雅黑", bulletFontSize=9.5, bulletOffsetY=1, spaceAfter=5)
    标记 = "□" if 类型 == "清单" else "•"
    return ListFlowable(条目, bulletType="bullet", bulletChar=标记, leftIndent=22, bulletFontName="微软雅黑", bulletFontSize=9.5, bulletOffsetY=1, spaceAfter=5)


def 生成PDF():
    注册PDF字体()
    样式们 = PDF样式()
    框 = Frame(72, 54, letter[0] - 144, letter[1] - 108, leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
    模板 = PageTemplate(id="报告", frames=[框], onPage=PDF页眉页脚)
    文档 = BaseDocTemplate(str(PDF路径), pagesize=letter, leftMargin=72, rightMargin=72, topMargin=54, bottomMargin=54, title="PETPET 产品与工程综合优化报告", author="PETPET 项目评审")
    文档.addPageTemplates([模板])
    流 = [Spacer(1, 14)]
    流.append(Paragraph("内部评审｜安卓原型到内测产品", ParagraphStyle("引导", parent=样式们["正文"], fontName="微软雅黑粗体", fontSize=9.5, leading=13, textColor=colors.HexColor("#2E74B5"), spaceAfter=3)))
    流.append(Paragraph("PETPET 产品与工程综合优化报告", 样式们["标题"]))
    流.append(Paragraph("从本地原型走向可内测产品的优先级、风险与验收基线", 样式们["副标题"]))
    for 标签, 文本 in 报告元数据.items():
        流.append(Paragraph(f"<b>{转义段落文本(标签)}：</b>{转义段落文本(文本)}", 样式们["元数据"]))
    流.append(Spacer(1, 10))
    流.append(Paragraph("<b>核心判断：</b>视觉与核心交互原型已经形成，但当前不具备正式上线条件。应先修复可复现构建与测试，再完成服务端权威数据、合规悬浮、智能聊天安全和社区治理闭环。", 样式们["提示"]))
    流.extend(PDF表格({"标题": "项目快照", "表头": ["项目", "取证结论"], "行": 首页快照, "列宽": [1.25, 5.25]}, 样式们))
    流.append(PageBreak())
    流.append(Paragraph("阅读导航", 样式们["二级"]))
    流.append(PDF列表(阅读导航, 样式们["列表"], "编号"))

    for 章节数据 in 章节:
        流.append(Paragraph(转义段落文本(章节数据["标题"]), 样式们["一级"]))
        for 项 in 章节数据["内容"]:
            类型 = 项[0]
            if 类型 == "正文":
                流.append(Paragraph(转义段落文本(项[1]), 样式们["正文"]))
            elif 类型 == "小标题":
                流.append(Paragraph(转义段落文本(项[1]), 样式们["二级"]))
            elif 类型 == "三级标题":
                流.append(Paragraph(转义段落文本(项[1]), 样式们["三级"]))
            elif 类型 == "提示":
                流.append(Paragraph(f"<b>{转义段落文本(项[1])}：</b>{转义段落文本(项[2])}", 样式们["提示"]))
            elif 类型 == "表格":
                流.extend(PDF表格(项[1], 样式们))
            elif 类型 in ("项目符号", "编号", "清单", "来源"):
                列表样式 = 样式们["来源"] if 类型 == "来源" else 样式们["列表"]
                列表类型 = "编号" if 类型 in ("编号", "来源") else 类型
                流.append(PDF列表(项[1], 列表样式, 列表类型))
            else:
                raise ValueError(f"未知内容块：{类型}")
    文档.build(流)
    return PDF路径


def 审计文档(路径: Path):
    文档 = Document(路径)
    问题 = []
    节 = 文档.sections[0]
    预期 = {
        "page_width": 12240,
        "page_height": 15840,
        "top_margin": 1440,
        "bottom_margin": 1440,
        "left_margin": 1440,
        "right_margin": 1440,
    }
    for 名称, 数值 in 预期.items():
        实际 = round(int(getattr(节, 名称)) / 635)
        if 实际 != 数值:
            问题.append(f"页面参数 {名称}={实际}，预期 {数值}")
    if round(int(节.header_distance) / 635) != 708 or round(int(节.footer_distance) / 635) != 708:
        问题.append("页眉或页脚距离不符合 0.492 英寸")

    for 表索引, 表格 in enumerate(文档.tables, 1):
        属性 = 表格._tbl.tblPr
        宽度 = 属性.find(qn("w:tblW"))
        缩进 = 属性.find(qn("w:tblInd"))
        if 宽度 is None or 宽度.get(qn("w:w")) != "9360" or 宽度.get(qn("w:type")) != "dxa":
            问题.append(f"表 {表索引} 的 tblW 不正确")
        if 缩进 is None or 缩进.get(qn("w:w")) != "120":
            问题.append(f"表 {表索引} 的 tblInd 不正确")
        网格 = [int(x.get(qn("w:w"))) for x in 表格._tbl.tblGrid.findall(qn("w:gridCol"))]
        if sum(网格) != 9360:
            问题.append(f"表 {表索引} 的网格总宽为 {sum(网格)}")
        for 行索引, 行 in enumerate(表格.rows, 1):
            for 列索引, 单元格 in enumerate(行.cells, 1):
                宽度节点 = 单元格._tc.get_or_add_tcPr().find(qn("w:tcW"))
                if 宽度节点 is None or int(宽度节点.get(qn("w:w"))) != 网格[列索引 - 1]:
                    问题.append(f"表 {表索引} 第 {行索引} 行第 {列索引} 列宽不一致")

    假列表 = []
    模式 = re.compile(r"^(?:[•●▪◦-]|\d+[\.、])\s*")
    for 索引, 段落 in enumerate(文档.paragraphs, 1):
        是标题 = 段落.style is not None and 段落.style.name.startswith("Heading")
        if 模式.match(段落.text.strip()) and not 是标题 and 段落._p.pPr is not None and 段落._p.pPr.numPr is None:
            假列表.append(索引)
    if 假列表:
        问题.append(f"发现疑似假列表段落：{假列表[:10]}")

    with zipfile.ZipFile(路径) as 压缩包:
        文档XML = 压缩包.read("word/document.xml").decode("utf-8")
        样式XML = 压缩包.read("word/styles.xml").decode("utf-8")
        编号XML = 压缩包.read("word/numbering.xml").decode("utf-8")
        if 'w:type="pct"' in 文档XML:
            问题.append("发现百分比表格宽度")
        if "w:trHeight" in 文档XML:
            问题.append("发现固定或指定行高")
        if "•" not in 编号XML or "□" not in 编号XML or "%1." not in 编号XML:
            问题.append("自定义项目符号、编号或清单定义缺失")
        for 令牌 in ("w:before=\"320\"", "w:after=\"160\"", "w:before=\"240\"", "w:after=\"120\""):
            if 令牌 not in 样式XML:
                问题.append(f"样式令牌缺失：{令牌}")

    结果 = {
        "文件": str(路径),
        "段落数": len(文档.paragraphs),
        "表格数": len(文档.tables),
        "问题": 问题,
    }
    质量检查目录.mkdir(parents=True, exist_ok=True)
    审计路径 = 质量检查目录 / "DOCX_结构审计.txt"
    审计路径.write_text("\n".join([
        f"文件：{结果['文件']}",
        f"段落数：{结果['段落数']}",
        f"表格数：{结果['表格数']}",
        "审计结果：" + ("通过" if not 问题 else "发现问题"),
        *[f"问题：{问题项}" for 问题项 in 问题],
    ]), encoding="utf-8")
    if 问题:
        raise RuntimeError("DOCX 结构审计未通过：" + "；".join(问题))
    return 结果


def 主程序():
    质量检查目录.mkdir(parents=True, exist_ok=True)
    生成文档()
    审计文档(文档路径)
    生成PDF()
    print(f"已生成：{文档路径}")
    print(f"已生成：{PDF路径}")


if __name__ == "__main__":
    主程序()
