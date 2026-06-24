export const adminMockUsers = [
  {
    user_id: 1001,
    username: 'xiaodai',
    phone: '13800000001',
    email: 'xiaodai@example.com',
    avatar: '',
    role: 'patient',
    status: 'active',
    create_time: '2026-06-18 09:20:00',
    last_login_time: '2026-06-24 08:40:00',
    profile_summary: '女，32岁，BMI 25.1，存在糖尿病家族史。',
    metric_summary: '空腹血糖 6.8 mmol/L，餐后血糖 9.4 mmol/L。',
    plan_count: 3,
    article_favorites: 5
  },
  {
    user_id: 1002,
    username: 'chenwei',
    phone: '13800000002',
    email: 'chenwei@example.com',
    avatar: '',
    role: 'patient',
    status: 'active',
    create_time: '2026-06-19 11:15:00',
    last_login_time: '2026-06-23 21:30:00',
    profile_summary: '男，45岁，腰围偏高，运动不足。',
    metric_summary: '空腹血糖 7.1 mmol/L，血压 136/86 mmHg。',
    plan_count: 2,
    article_favorites: 2
  },
  {
    user_id: 1003,
    username: 'linyu',
    phone: '13800000003',
    email: 'linyu@example.com',
    avatar: '',
    role: 'patient',
    status: 'disabled',
    create_time: '2026-06-20 15:40:00',
    last_login_time: '2026-06-22 10:15:00',
    profile_summary: '女，29岁，近期未补充健康档案。',
    metric_summary: '健康数据缺失。',
    plan_count: 0,
    article_favorites: 1
  },
  {
    user_id: 9001,
    username: 'admin',
    phone: '13900000000',
    email: 'admin@example.com',
    avatar: '',
    role: 'admin',
    status: 'active',
    create_time: '2026-06-10 10:00:00',
    last_login_time: '2026-06-24 09:00:00',
    profile_summary: '系统管理员账号。',
    metric_summary: '不适用。',
    plan_count: 0,
    article_favorites: 0
  }
]

export const adminMockLifePlans = [
  {
    plan_id: 3001,
    user_id: 1001,
    username: 'xiaodai',
    phone: '13800000001',
    plan_title: '个性化控糖生活方案',
    plan_goal: '控糖和减重',
    risk_level: '中风险',
    status: 'active',
    call_status: 'success',
    input_summary: '用户目标：控糖和减重；避免项：海鲜、高强度跑步。',
    summary: '以稳定血糖和体重控制为目标，建议规律饮食、低强度有氧和睡眠管理。',
    plan_json: {
      risk_summary: '当前为中风险，需重点关注餐后血糖波动。',
      diet_plan: {
        breakfast: '燕麦粥、鸡蛋、少量坚果',
        lunch: '糙米饭、清蒸鱼、绿叶蔬菜',
        dinner: '杂粮饭、豆腐、凉拌蔬菜',
        snack: '无糖酸奶或低糖水果'
      },
      exercise_plan: {
        exercise_type: '快走 + 拉伸',
        frequency: '每周 5 次',
        duration: '每次 30 分钟',
        intensity: '低到中等强度',
        precautions: '避免高强度跑步'
      },
      daily_schedule: [
        { day: '第 1 天', time: '07:30', type: 'diet', task: '早餐按低 GI 主食搭配蛋白质' },
        { day: '第 1 天', time: '18:30', type: 'exercise', task: '晚饭后步行 20 分钟' },
        { day: '第 2 天', time: '15:00', type: 'check', task: '记录餐后 2 小时血糖' }
      ],
      health_tips: ['不建议完全不吃主食', '每坐 1 小时起身活动'],
      medical_warning: 'AI 建议仅供参考，不能替代线下诊疗。'
    },
    checkin_tasks_json: [
      { task_type: 'diet', task_name: '早餐控糖打卡', description: '记录早餐主食和蛋白质搭配' },
      { task_type: 'exercise', task_name: '饭后步行打卡', description: '记录饭后步行时长' }
    ],
    error_message: '',
    create_time: '2026-06-24 09:30:00',
    update_time: '2026-06-24 09:35:00'
  },
  {
    plan_id: 3002,
    user_id: 1002,
    username: 'chenwei',
    phone: '13800000002',
    plan_title: '血糖稳定生活干预方案',
    plan_goal: '稳定血糖',
    risk_level: '高风险',
    status: 'history',
    call_status: 'success',
    input_summary: '用户目标：稳定血糖；避免项：膝盖高冲击运动。',
    summary: '重点控制高油高糖饮食，增加低冲击运动。',
    plan_json: '{"risk_summary":"高风险，建议尽快完善线下评估。","diet_plan":{"breakfast":"全麦面包、鸡蛋、无糖豆浆","lunch":"杂粮饭、鸡胸肉、蔬菜","dinner":"少油蔬菜、鱼肉、半份主食"},"exercise_plan":{"exercise_type":"椭圆机或游泳","frequency":"每周4次","duration":"每次25分钟","intensity":"中等偏低"},"health_tips":["关注血压和腰围变化"],"medical_warning":"如指标明显异常，请及时线下就医。"}',
    checkin_tasks_json: '[{"task_type":"diet","task_name":"晚餐减油打卡"},{"task_type":"exercise","task_name":"低冲击运动打卡"}]',
    error_message: '',
    create_time: '2026-06-22 17:10:00',
    update_time: '2026-06-22 17:15:00'
  },
  {
    plan_id: 3003,
    user_id: 1003,
    username: 'linyu',
    phone: '13800000003',
    plan_title: '生活方案生成失败记录',
    plan_goal: '改善饮食习惯',
    risk_level: '未知',
    status: 'failed',
    call_status: 'failed',
    input_summary: '用户目标：改善饮食习惯；健康档案缺失。',
    summary: '',
    plan_json: null,
    checkin_tasks_json: null,
    error_message: '缺少成功风险评估结果，无法生成生活方案。',
    create_time: '2026-06-21 13:25:00',
    update_time: '2026-06-21 13:25:00'
  }
]

export const articleCategories = [
  { label: '饮食指导', value: 'diet' },
  { label: '运动指南', value: 'exercise' },
  { label: '日常习惯', value: 'habit' },
  { label: '糖尿病科普', value: 'science' },
  { label: '并发症预防', value: 'complication' },
  { label: '控糖误区', value: 'mistake' }
]

export const adminMockArticles = [
  {
    article_id: 2001,
    title: '控糖饮食的三个关键原则',
    category: 'diet',
    cover_image: '',
    summary: '合理控制碳水摄入，搭配优质蛋白和蔬菜。',
    content: '控糖饮食不是完全不吃主食，而是选择低升糖指数主食，控制总量，并搭配足量蔬菜和优质蛋白。',
    status: 'published',
    view_count: 120,
    is_recommended: 1,
    sort_order: 10,
    create_time: '2026-06-20 09:30:00',
    update_time: '2026-06-20 09:30:00'
  },
  {
    article_id: 2002,
    title: '饭后散步对血糖有什么帮助',
    category: 'exercise',
    cover_image: '',
    summary: '适度饭后步行有助于改善餐后血糖波动。',
    content: '饭后低强度散步可以帮助肌肉利用葡萄糖。建议从 10 到 15 分钟开始，避免高强度运动。',
    status: 'published',
    view_count: 86,
    is_recommended: 1,
    sort_order: 20,
    create_time: '2026-06-19 18:20:00',
    update_time: '2026-06-19 18:20:00'
  },
  {
    article_id: 2003,
    title: '久坐人群如何降低糖尿病风险',
    category: 'habit',
    cover_image: '',
    summary: '避免长时间久坐，每小时起身活动。',
    content: '久坐会影响身体代谢。建议每坐 45 到 60 分钟起身活动几分钟，配合规律睡眠。',
    status: 'draft',
    view_count: 0,
    is_recommended: 0,
    sort_order: 30,
    create_time: '2026-06-18 14:10:00',
    update_time: '2026-06-18 14:10:00'
  }
]

export const adminMockHomeContents = [
  {
    content_id: 4001,
    content_type: 'banner',
    title: '每日控糖小课堂',
    subtitle: '饮食、运动、习惯一站式科普',
    image_url: '',
    link_type: 'article',
    link_value: '2001',
    sort_order: 1,
    status: 'enabled'
  },
  {
    content_id: 4002,
    content_type: 'ai_doctor_card',
    title: 'AI 控糖助手',
    subtitle: '糖尿病相关问题随时咨询',
    image_url: '',
    link_type: 'chat',
    link_value: 'chat',
    sort_order: 2,
    status: 'enabled'
  }
]
