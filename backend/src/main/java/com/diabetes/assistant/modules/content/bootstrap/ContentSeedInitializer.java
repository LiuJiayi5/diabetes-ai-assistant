package com.diabetes.assistant.modules.content.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.assistant.common.config.UploadProperties;
import com.diabetes.assistant.modules.content.entity.Article;
import com.diabetes.assistant.modules.content.entity.HomeContent;
import com.diabetes.assistant.modules.content.mapper.ArticleMapper;
import com.diabetes.assistant.modules.content.mapper.HomeContentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ContentSeedInitializer implements ApplicationRunner {

    private static final String STATUS_PUBLISHED = "published";
    private static final String STATUS_ENABLED = "enabled";
    private static final String TYPE_BANNER = "banner";
    private static final String SEED_DIR = "seed/content";

    private final ArticleMapper articleMapper;
    private final HomeContentMapper homeContentMapper;
    private final UploadProperties uploadProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureSeedImages();
        seedArticles();
        seedBanners();
    }

    private void seedArticles() {
        repairMissingImagePaths();
        for (SeedArticle seed : articleSeeds()) {
            Long categoryCount = articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                    .eq(Article::getCategory, seed.category())
                    .eq(Article::getStatus, STATUS_PUBLISHED));
            if (categoryCount != null && categoryCount >= 5) {
                continue;
            }

            Long existing = articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                    .eq(Article::getTitle, seed.title()));
            if (existing != null && existing > 0) {
                continue;
            }

            LocalDateTime now = LocalDateTime.now().minusDays(seed.sortOrder() % 18L);
            Article article = new Article();
            article.setTitle(seed.title());
            article.setCategory(seed.category());
            article.setCoverImage("/uploads/" + SEED_DIR + "/" + seed.imageName());
            article.setSummary(seed.summary());
            article.setContent(seed.content());
            article.setStatus(STATUS_PUBLISHED);
            article.setViewCount(seed.viewCount());
            article.setIsRecommended(seed.recommended() ? 1 : 0);
            article.setSortOrder(seed.sortOrder());
            article.setCreateTime(now);
            article.setUpdateTime(now);
            articleMapper.insert(article);
        }
    }

    private void repairMissingImagePaths() {
        updateArticleImage("控糖饮食的三餐搭配", "diet-plate.svg");
        updateArticleImage("适合糖尿病前期的运动原则", "exercise-walk.svg");
        updateArticleImage("糖尿病常见误区：只看空腹血糖", "mistake-drink.svg");
        updateHomeImage("今日控糖计划", "banner-prevention.svg");
        updateHomeImage("AI 医师咨询", "banner-ai.svg");
    }

    private void updateArticleImage(String title, String imageName) {
        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getTitle, title)
                .last("LIMIT 1"));
        if (article == null) {
            return;
        }
        article.setCoverImage("/uploads/" + SEED_DIR + "/" + imageName);
        article.setUpdateTime(LocalDateTime.now());
        articleMapper.updateById(article);
    }

    private void updateHomeImage(String title, String imageName) {
        HomeContent content = homeContentMapper.selectOne(new LambdaQueryWrapper<HomeContent>()
                .eq(HomeContent::getTitle, title)
                .last("LIMIT 1"));
        if (content == null) {
            return;
        }
        content.setImageUrl("/uploads/" + SEED_DIR + "/" + imageName);
        content.setUpdateTime(LocalDateTime.now());
        homeContentMapper.updateById(content);
    }

    private void seedBanners() {
        List<SeedBanner> banners = bannerSeeds();
        for (SeedBanner seed : banners) {
            HomeContent existing = homeContentMapper.selectOne(new LambdaQueryWrapper<HomeContent>()
                    .eq(HomeContent::getContentType, TYPE_BANNER)
                    .eq(HomeContent::getTitle, seed.title())
                    .last("LIMIT 1"));
            if (existing != null) {
                existing.setSubtitle(seed.subtitle());
                existing.setImageUrl("/uploads/" + SEED_DIR + "/" + seed.imageName());
                existing.setLinkType(seed.linkType());
                existing.setLinkValue(seed.linkValue());
                existing.setSortOrder(seed.sortOrder());
                existing.setStatus(STATUS_ENABLED);
                existing.setUpdateTime(LocalDateTime.now());
                homeContentMapper.updateById(existing);
                continue;
            }

            long enabledBannerCount = homeContentMapper.selectCount(new LambdaQueryWrapper<HomeContent>()
                    .eq(HomeContent::getContentType, TYPE_BANNER)
                    .eq(HomeContent::getStatus, STATUS_ENABLED));
            if (enabledBannerCount >= 3) {
                continue;
            }

            LocalDateTime now = LocalDateTime.now();
            HomeContent content = new HomeContent();
            content.setContentType(TYPE_BANNER);
            content.setTitle(seed.title());
            content.setSubtitle(seed.subtitle());
            content.setImageUrl("/uploads/" + SEED_DIR + "/" + seed.imageName());
            content.setLinkType(seed.linkType());
            content.setLinkValue(seed.linkValue());
            content.setSortOrder(seed.sortOrder());
            content.setStatus(STATUS_ENABLED);
            content.setCreateTime(now);
            content.setUpdateTime(now);
            homeContentMapper.insert(content);
        }
    }

    private void ensureSeedImages() {
        Path basePath = Path.of(uploadProperties.getRootDir()).toAbsolutePath().normalize().resolve(SEED_DIR);
        try {
            Files.createDirectories(basePath);
            for (SeedImage image : imageSeeds()) {
                Path target = basePath.resolve(image.filename());
                if (Files.exists(target)) {
                    continue;
                }
                Files.writeString(target, buildSvg(image), StandardCharsets.UTF_8);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("健康资讯初始化图片生成失败", ex);
        }
    }

    private List<SeedImage> imageSeeds() {
        return List.of(
                new SeedImage("diet-plate.svg", "#BCEBCF", "#C7E8F7", "M148 270c68-34 148-26 210 19", "M175 205h140", "M188 236h112"),
                new SeedImage("diet-grain.svg", "#CDEFD8", "#E7F8EF", "M170 290c42-70 112-104 190-90", "M184 214c42 28 86 28 132 0", "M206 250h94"),
                new SeedImage("diet-vegetable.svg", "#AEE8C7", "#BFE9F2", "M150 250c54-58 152-68 214-8", "M178 210c38 22 85 22 124 0", "M200 248h128"),
                new SeedImage("diet-fruit.svg", "#DDF7E9", "#BEEAF2", "M160 280c48-42 138-42 186 0", "M202 188c20 28 56 28 76 0", "M188 230h148"),
                new SeedImage("diet-breakfast.svg", "#EAF8F4", "#C7E8F7", "M142 284c78-46 172-46 250 0", "M178 220h172", "M210 250h112"),
                new SeedImage("exercise-walk.svg", "#BFE3F5", "#BCEBCF", "M165 290c46-44 108-40 160-6", "M214 176l38 46 54-24", "M192 235h126"),
                new SeedImage("exercise-jog.svg", "#C7E8F7", "#DDF7E9", "M150 292c62-50 158-48 224 4", "M206 182l52 32 54-42", "M198 240h136"),
                new SeedImage("exercise-stretch.svg", "#BEEAF2", "#E8F8F2", "M154 286c78-38 154-40 230-4", "M196 190h128", "M222 224l68 46"),
                new SeedImage("exercise-elder.svg", "#DDF7E9", "#BFE9F2", "M155 290c64-42 150-40 218 2", "M196 194l42 34 64-18", "M216 252h86"),
                new SeedImage("exercise-home.svg", "#E8F4FA", "#BCEBCF", "M154 284c56-42 142-45 224 0", "M190 202h146", "M226 232v42"),
                new SeedImage("habit-sleep.svg", "#EDE8FC", "#D8EEF9", "M158 280c58-36 144-34 206 6", "M188 206h132", "M210 238h90"),
                new SeedImage("habit-water.svg", "#D8EEF9", "#DDF7E9", "M158 284c48-54 148-58 206 0", "M220 176c18 30 56 60 56 92", "M190 236h148"),
                new SeedImage("habit-routine.svg", "#EAF5FA", "#CFEFD9", "M154 286c76-42 158-42 224 0", "M198 198h128", "M198 232h92"),
                new SeedImage("habit-emotion.svg", "#F3EEFF", "#DDF7E9", "M152 282c54-44 148-46 222 0", "M200 205c40 28 86 28 126 0", "M222 244h82"),
                new SeedImage("habit-monitor.svg", "#E8F7EE", "#E2F3FA", "M156 286c62-40 150-42 216 0", "M188 206h152", "M210 242l38-24 30 22 42-42"),
                new SeedImage("science-meter.svg", "#E5F6EE", "#E4F3FB", "M158 286c58-44 142-44 214 0", "M188 210h150", "M214 244h84"),
                new SeedImage("science-doctor.svg", "#EAF8F4", "#BEEAF2", "M150 286c70-42 156-42 232 0", "M198 202h132", "M216 234h96"),
                new SeedImage("science-insulin.svg", "#DDF7E9", "#C7E8F7", "M152 286c68-42 156-42 226 0", "M194 196l112 68", "M216 218l64-38"),
                new SeedImage("science-check.svg", "#CFEFD9", "#EAF5FA", "M154 282c64-38 156-40 224 2", "M190 214h150", "M212 250l30 28 70-82"),
                new SeedImage("science-family.svg", "#E4F3FB", "#DDF7E9", "M150 286c70-44 160-44 232 0", "M190 218h148", "M224 190h80"),
                new SeedImage("complication-heart.svg", "#FEF3E2", "#EAF8F4", "M154 284c62-42 152-42 220 0", "M205 216c20-42 76-42 96 0", "M196 252h132"),
                new SeedImage("complication-eye.svg", "#FEF8EC", "#BEEAF2", "M152 284c70-42 158-42 224 0", "M182 226c52-48 120-48 172 0", "M232 226h72"),
                new SeedImage("complication-kidney.svg", "#F7E9CC", "#DDF7E9", "M154 286c62-42 152-44 224 0", "M204 198c-22 18-26 64 6 86", "M300 198c22 18 26 64-6 86"),
                new SeedImage("complication-foot.svg", "#FEF3E2", "#E4F3FB", "M154 286c58-38 142-42 218 0", "M190 236c34 28 78 28 134 10", "M214 202h92"),
                new SeedImage("complication-pressure.svg", "#F9EEDA", "#EAF8F4", "M154 286c70-40 158-42 224 0", "M188 218h146", "M212 252h92"),
                new SeedImage("mistake-drink.svg", "#EEF2FE", "#DDF7E9", "M156 286c62-40 146-42 220 0", "M220 190h72", "M208 222h96"),
                new SeedImage("mistake-diet.svg", "#F3EEFF", "#EAF5FA", "M154 286c72-40 160-42 224 0", "M184 218h150", "M208 250h94"),
                new SeedImage("mistake-rice.svg", "#E8F4FA", "#F8EADA", "M154 286c70-42 160-42 224 0", "M184 222c44 28 106 28 150 0", "M212 252h94"),
                new SeedImage("mistake-medicine.svg", "#EAF5FA", "#DDF7E9", "M156 286c66-42 154-42 222 0", "M198 202h132", "M224 232h80"),
                new SeedImage("mistake-sleep.svg", "#EDE8FC", "#BEEAF2", "M154 286c72-42 160-42 226 0", "M192 216h140", "M214 248h86"),
                new SeedImage("banner-prevention.svg", "#AEE8C7", "#BFE9F2", "M122 282c95-56 214-58 306 0", "M178 210h188", "M214 248h124"),
                new SeedImage("banner-diet.svg", "#BCEBCF", "#DDF7E9", "M118 286c104-62 232-62 328 0", "M178 216h214", "M218 252h132"),
                new SeedImage("banner-ai.svg", "#BFE3F5", "#EAF8F4", "M122 284c98-56 220-58 310 0", "M188 210h198", "M226 246l44-28 34 26 52-54")
        );
    }

    private String buildSvg(SeedImage image) {
        return """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 520 340" role="img">
                  <defs>
                    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0" stop-color="%s"/>
                      <stop offset="1" stop-color="%s"/>
                    </linearGradient>
                    <filter id="shadow" x="-20%%" y="-20%%" width="140%%" height="140%%">
                      <feDropShadow dx="0" dy="12" stdDeviation="18" flood-color="#6FAF96" flood-opacity="0.16"/>
                    </filter>
                  </defs>
                  <rect width="520" height="340" rx="34" fill="url(#bg)"/>
                  <circle cx="418" cy="74" r="82" fill="#FFFFFF" opacity="0.23"/>
                  <circle cx="98" cy="292" r="70" fill="#FFFFFF" opacity="0.18"/>
                  <rect x="122" y="86" width="276" height="182" rx="34" fill="#FFFFFF" opacity="0.72" filter="url(#shadow)"/>
                  <circle cx="196" cy="150" r="34" fill="#6FCF97" opacity="0.86"/>
                  <path d="M185 150l16 16 34-44" fill="none" stroke="#FFFFFF" stroke-width="12" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="%s" fill="none" stroke="#4FB783" stroke-width="14" stroke-linecap="round" opacity="0.84"/>
                  <path d="%s" fill="none" stroke="#5EA8C7" stroke-width="12" stroke-linecap="round" opacity="0.78"/>
                  <path d="%s" fill="none" stroke="#84CFA9" stroke-width="12" stroke-linecap="round" opacity="0.82"/>
                  <circle cx="348" cy="146" r="14" fill="#F4C87A" opacity="0.9"/>
                  <circle cx="374" cy="184" r="9" fill="#7FC8E8" opacity="0.9"/>
                </svg>
                """.formatted(image.startColor(), image.endColor(), image.pathOne(), image.pathTwo(), image.pathThree());
    }

    private List<SeedBanner> bannerSeeds() {
        return List.of(
                new SeedBanner("从今天开始稳稳控糖", "饮食、运动和监测一起做，逐步建立适合自己的健康节奏。", "banner-prevention.svg", "none", "", 1),
                new SeedBanner("低糖餐盘也可以很好吃", "学会搭配全谷物、蔬菜和优质蛋白，让每一餐更安心。", "banner-diet.svg", "none", "", 2),
                new SeedBanner("AI 助手陪你记录健康变化", "结合健康档案和生活方案，及时发现需要关注的指标。", "banner-ai.svg", "life_plan", "", 3)
        );
    }

    private List<SeedArticle> articleSeeds() {
        Map<String, String> imageMap = Map.of(
                "diet", "diet-plate.svg",
                "exercise", "exercise-walk.svg",
                "habit", "habit-routine.svg",
                "science", "science-meter.svg",
                "complication", "complication-heart.svg",
                "mistake", "mistake-drink.svg"
        );
        return List.of(
                article("diet", "一日三餐如何安排更稳糖", "从主食、蛋白质和蔬菜比例入手，帮助餐后血糖更平稳。", imageMap.get("diet"), 120, 1, true,
                        "很多人一听到控糖，就会先想到少吃甚至不吃主食。更可持续的做法，是让每一餐结构更清楚，既不过量，也不把身体需要的营养全部去掉。\n" +
                                "主食要有选择。可以把白米饭、白面条的一部分换成燕麦、糙米、杂豆或薯类，分量以个人血糖反应和医生建议为准。不要因为担心血糖就长期完全不吃主食。\n" +
                                "蛋白质和蔬菜要跟上。每餐安排鱼、禽、蛋、豆制品或瘦肉等优质蛋白，再搭配足量深色蔬菜，有助于增加饱腹感，减少餐后大幅波动。\n" +
                                "进餐节奏也重要。尽量固定吃饭时间，少喝含糖饮料，甜点放在偶尔少量，而不是每天习惯性加餐。如餐后血糖反复偏高，请记录饮食并咨询医生或营养师。\n" +
                                "总结来说，稳糖饮食不是严苛禁食，而是长期可执行的餐盘管理。"),
                article("diet", "全谷物怎么吃才更适合控糖", "了解全谷物的选择、替换比例和常见误区。", "diet-grain.svg", 98, 2, true,
                        "全谷物保留了更多膳食纤维和营养成分，相比精白米面，通常更适合作为日常主食的一部分。但全谷物也含有碳水化合物，吃法仍然需要讲究。\n" +
                                "先从替换开始。可以把一餐主食中的三分之一到二分之一换成糙米、燕麦、荞麦或杂豆，不必一开始就全部替换，避免肠胃不适。\n" +
                                "注意总量不超标。全谷物更健康，不代表可以无限量吃。建议结合餐后血糖、体重目标和日常活动量调整分量。\n" +
                                "搭配比单吃更重要。全谷物配蔬菜和蛋白质，比单独吃一大碗杂粮饭更利于餐后平稳。烹饪时少油少糖，避免把粗粮做成甜点。\n" +
                                "如果有肾病、胃肠疾病等特殊情况，主食调整前应先咨询医生。"),
                article("diet", "外出就餐的控糖点餐技巧", "在餐馆吃饭也能通过选择和分量控制降低波动。", "diet-vegetable.svg", 86, 3, false,
                        "外出就餐往往油盐偏多、主食分量不易控制，但只要提前有策略，仍然可以把风险降下来。\n" +
                                "点菜先看烹饪方式。优先选择清蒸、炖煮、凉拌或少油快炒，少选油炸、糖醋、拔丝和浓酱菜。汤汁和酱料通常含盐、糖或油较多，建议少蘸。\n" +
                                "主食可以主动减量。米饭、面条、饼类不要默认吃完，可先盛半份，搭配蔬菜和蛋白质后再看饱腹感。\n" +
                                "饮料选择要简单。白水、无糖茶更合适，鲜榨果汁也可能含较多糖分，不建议当作日常饮品。\n" +
                                "如果聚餐时间较晚或饮食明显变化，建议按医嘱监测血糖，出现不适及时处理。"),
                article("diet", "低糖水果应该怎么选", "水果不是完全不能吃，关键在种类、分量和时间。", "diet-fruit.svg", 112, 4, false,
                        "水果含有维生素、矿物质和膳食纤维，也含天然糖分。控糖人群不必一概拒绝水果，但需要学会选择和分量控制。\n" +
                                "优先选择升糖相对平稳的水果。例如苹果、梨、柚子、莓类等通常比含糖饮料和甜点更适合。成熟度很高、口感特别甜的水果要控制量。\n" +
                                "把水果放在两餐之间。不要在已经吃饱后马上大量吃水果，也不要把水果榨汁喝。完整水果的纤维保留更多，更利于控制速度和分量。\n" +
                                "观察个人反应。不同人对同一种水果的血糖反应不同，可以记录食用量和餐后血糖变化。\n" +
                                "如血糖近期明显偏高，应先遵医嘱稳定指标，再决定水果安排。"),
                article("diet", "早餐怎样吃更不容易饿", "早餐要避免只有粥或甜面包，注意蛋白质和纤维。", "diet-breakfast.svg", 76, 5, false,
                        "早餐会影响上午的精神状态和血糖波动。只喝白粥、只吃甜面包，容易让餐后血糖上升较快，也可能很快感到饥饿。\n" +
                                "主食选择更耐饿。燕麦、全麦面包、杂粮馒头等可以作为选择，但仍要注意分量。白粥如果要吃，建议搭配蛋白质和蔬菜，而不是单独一大碗。\n" +
                                "加入优质蛋白。鸡蛋、无糖酸奶、豆浆、豆腐或少量瘦肉，都能提高饱腹感。\n" +
                                "避开隐藏糖。许多早餐奶、调味酸奶、甜豆浆和糕点含糖较高，购买时要看配料和营养成分。\n" +
                                "早餐不是越少越好，规律、均衡、可坚持才更有价值。"),
                article("exercise", "饭后散步多久比较合适", "低强度步行有助于改善餐后状态，但要循序渐进。", "exercise-walk.svg", 132, 11, true,
                        "饭后活动不是剧烈运动，而是让身体从久坐状态中动起来。对许多人来说，饭后轻松散步比立刻坐下更有利于餐后血糖管理。\n" +
                                "时间从短开始。可以从饭后 20 到 30 分钟开始，步行 10 到 15 分钟，身体适应后再逐步延长。刚吃完不要马上快跑或做高强度训练。\n" +
                                "强度以能说话为宜。散步时微微发热、呼吸略加快即可。如果出现胸闷、头晕、心慌或明显低血糖表现，应立即停止并处理。\n" +
                                "坚持比一次走很久更重要。每周多次规律活动，比偶尔一次长时间运动更容易形成稳定习惯。\n" +
                                "如已有心血管、足部或关节问题，请先咨询医生选择合适运动方式。"),
                article("exercise", "适合初学者的居家拉伸", "用温和动作改善活动度，减少久坐带来的不适。", "exercise-stretch.svg", 91, 12, false,
                        "很多人因为工作忙或天气原因无法外出运动，居家拉伸可以作为日常活动的起点。它不能替代所有有氧运动，但能帮助身体从僵硬状态恢复。\n" +
                                "先做肩颈和背部放松。每个动作保持 10 到 20 秒，不要憋气，不追求疼痛感。动作幅度以舒适为准。\n" +
                                "加入下肢活动。靠墙提踵、坐姿抬腿、髋部轻柔拉伸，都适合从低强度开始。注意地面防滑。\n" +
                                "安排固定时间。早晨起床后、午休后或晚饭后都可以，每次 10 分钟也有意义。\n" +
                                "如拉伸时出现明显疼痛、麻木或头晕，应停止并寻求专业建议。"),
                article("exercise", "老年人控糖运动要注意什么", "安全、稳定、可持续，比追求强度更重要。", "exercise-elder.svg", 105, 13, true,
                        "老年人运动控糖要把安全放在第一位。合适的运动能帮助改善体力和代谢状态，但不应盲目追求速度或强度。\n" +
                                "选择低冲击活动。步行、太极、柔韧训练、轻阻力训练都可以根据身体情况尝试。运动鞋要合脚，场地要平整。\n" +
                                "注意运动前后状态。不要空腹长时间运动，随身携带糖果或饼干以备低血糖处理。运动前后检查足部是否有磨损。\n" +
                                "强度逐步增加。每次从 10 分钟开始也可以，适应后再延长。出现胸痛、气短、头晕等情况要立即停止。\n" +
                                "合并心脏病、眼底病变或足部问题的人，应在医生指导下制定运动计划。"),
                article("exercise", "力量训练对血糖管理有什么帮助", "适度抗阻训练有助于肌肉利用葡萄糖。", "exercise-home.svg", 84, 14, false,
                        "除了散步和慢跑，适度力量训练也值得关注。肌肉是身体利用葡萄糖的重要组织，保持肌肉量有助于长期代谢管理。\n" +
                                "从自身体重开始。靠墙俯卧撑、坐姿起立、弹力带划船等动作比较容易入门。每次选择 4 到 6 个动作，每个动作少量多组。\n" +
                                "动作质量优先。速度放慢，避免憋气和突然发力。训练后肌肉轻微酸胀可以接受，但关节疼痛不应硬扛。\n" +
                                "不要每天练同一部位。给肌肉恢复时间，和有氧运动交替安排更合适。\n" +
                                "如果有严重并发症或近期血糖波动明显，训练前要先咨询医生。"),
                article("exercise", "运动前后血糖监测怎么做", "了解自身反应，能让运动安排更安心。", "exercise-jog.svg", 118, 15, false,
                        "运动会影响血糖，但不同人的反应不完全一样。通过记录运动前后指标，可以更好地找到适合自己的时间、强度和补给方式。\n" +
                                "关注运动前状态。空腹、药物作用高峰期或身体不舒服时，不适合贸然增加运动量。使用降糖药或胰岛素的人尤其要注意低血糖风险。\n" +
                                "记录运动类型和时长。把步行、骑车、力量训练等和血糖变化一起记录，几周后就能看出规律。\n" +
                                "运动后不要马上大量进食。可以根据血糖和饥饿程度选择合适加餐，避免运动后补偿性摄入过多。\n" +
                                "如运动中反复出现低血糖或异常不适，应及时和医生沟通调整方案。"),
                article("habit", "规律睡眠对控糖为什么重要", "睡眠不足可能影响食欲、体重和血糖波动。", "habit-sleep.svg", 109, 21, true,
                        "控糖不只发生在餐桌和运动场，睡眠也会影响身体代谢。长期睡眠不足可能让食欲更难控制，白天更疲惫，也不利于坚持运动。\n" +
                                "先固定作息。尽量每天相近时间睡觉和起床，周末也不要大幅颠倒。睡前减少手机刺激，给身体一个放松过程。\n" +
                                "晚餐和夜宵要留意。太晚吃得过饱，或睡前频繁吃甜食，都可能影响睡眠和夜间血糖。\n" +
                                "识别睡眠问题。如果长期打鼾严重、白天嗜睡或夜间频繁醒来，建议线下就医评估。\n" +
                                "良好睡眠不能替代治疗，但它能让饮食、运动和监测更容易坚持。"),
                article("habit", "每天喝水也有控糖意义吗", "用无糖饮品替代甜饮料，是很实际的改变。", "habit-water.svg", 87, 22, false,
                        "喝水本身不是降糖药，但用白水替代含糖饮料，是减少糖摄入的直接方法。很多人的额外热量，正来自不经意喝下的甜饮。\n" +
                                "优先选择白水。无糖茶、淡咖啡也可以根据个人情况选择，但不要把果汁、奶茶、甜咖啡当作日常水分来源。\n" +
                                "分散到一天里喝。不要等到很渴才一次大量饮水。天气炎热、运动后或出汗较多时，要适当补充。\n" +
                                "特殊人群要遵医嘱。心肾功能异常或医生要求限制饮水的人，不能简单照搬普通建议。\n" +
                                "把水杯放在手边，是一个很小但很有效的健康提醒。"),
                article("habit", "久坐人群的血糖管理小动作", "每小时起身活动几分钟，帮助打断久坐。", "habit-routine.svg", 101, 23, true,
                        "长时间坐着会减少肌肉活动，也容易让人忽略饮食和体重管理。对上班族来说，打断久坐是最容易开始的改变之一。\n" +
                                "设置起身提醒。每 45 到 60 分钟站起来 3 到 5 分钟，倒水、走动、伸展都可以。不要等到腰背酸痛才活动。\n" +
                                "利用碎片时间。接电话时站立，午休后散步，短距离尽量步行。小动作累积起来，也能改善日常活动量。\n" +
                                "配合餐后监测。如果发现久坐日餐后血糖更高，可以优先安排饭后轻步行。\n" +
                                "改变不需要一步到位，先减少连续久坐时间，就是很好的开始。"),
                article("habit", "情绪压力大时如何避免暴食", "压力会影响食欲，提前准备替代策略更稳妥。", "habit-emotion.svg", 74, 24, false,
                        "情绪和血糖管理关系密切。压力大时，有些人会想吃甜食或高油食物来缓解情绪，短期舒服，长期却可能增加控制难度。\n" +
                                "先识别触发点。记录自己什么时候最容易想吃，比如加班后、争吵后或睡前。知道原因，才更容易提前准备。\n" +
                                "准备替代动作。散步 10 分钟、深呼吸、喝水、和朋友聊一聊，都可以作为缓冲。真正饿了再选择合适加餐。\n" +
                                "不要用内疚解决问题。偶尔吃多了，不代表失败。下一餐回到规律节奏，并记录血糖变化即可。\n" +
                                "如果长期焦虑、抑郁或暴食难以控制，建议寻求专业帮助。"),
                article("habit", "家庭血糖记录怎样更有用", "记录时间、饮食和运动背景，比单个数字更有参考价值。", "habit-monitor.svg", 95, 25, false,
                        "很多人会测血糖，但记录不够完整，导致复诊时很难判断问题来自饮食、运动还是药物。一个有背景的记录更有价值。\n" +
                                "记录关键时间点。按医生建议记录空腹、餐后或睡前血糖，不必自行过度频繁测量。每次写清日期和时间。\n" +
                                "补充生活背景。当天吃了什么、是否运动、睡眠如何、有没有不适，都可能解释数字变化。\n" +
                                "看趋势，不只看一次。偶尔一次偏高先别慌，连续异常更需要重视，并带着记录咨询医生。\n" +
                                "记录的目的不是给自己压力，而是帮助找到更适合自己的管理方案。"),
                article("science", "糖尿病前期是什么意思", "了解风险阶段，越早调整生活方式越有机会改善。", "science-check.svg", 130, 31, true,
                        "糖尿病前期是指血糖已经高于理想范围，但尚未达到糖尿病诊断标准的状态。它提示身体代谢已经发出信号，需要认真对待。\n" +
                                "不要忽视风险。糖尿病前期不一定有明显症状，因此体检指标很重要。空腹血糖、餐后血糖和糖化血红蛋白都可能提供线索。\n" +
                                "生活方式调整很关键。合理饮食、规律运动、体重管理和睡眠改善，都可能帮助降低进一步发展的风险。\n" +
                                "定期复查。不要凭一次结果自行判断，也不要自行用药。应根据医生建议复查和评估。\n" +
                                "早发现不是坏消息，它意味着还有时间主动管理。"),
                article("science", "空腹血糖和餐后血糖看哪个", "两个指标反映不同侧面，结合起来更完整。", "science-meter.svg", 117, 32, true,
                        "空腹血糖和餐后血糖都很重要。只看其中一个，可能会漏掉一些波动情况。理解它们的意义，有助于更好地和医生沟通。\n" +
                                "空腹血糖反映基础状态。它通常受前一天晚餐、睡眠、肝糖输出等因素影响。如果连续偏高，需要关注整体管理。\n" +
                                "餐后血糖反映进餐反应。它和主食种类、分量、进餐速度、饭后活动都有关系。很多人空腹不高，但餐后波动明显。\n" +
                                "糖化血红蛋白看长期趋势。它不能替代日常监测，但能帮助了解一段时间的平均水平。\n" +
                                "指标解读要结合个人情况，具体目标请遵医嘱。"),
                article("science", "糖化血红蛋白为什么重要", "它能帮助了解近一段时间血糖总体控制情况。", "science-doctor.svg", 99, 33, false,
                        "糖化血红蛋白常被用来评估近 2 到 3 个月血糖总体情况。它不像一次指尖血糖那样容易受单餐影响，因此有重要参考价值。\n" +
                                "它看的是趋势。一次空腹或餐后血糖可能受当天状态影响，而糖化血红蛋白能帮助医生判断长期控制是否平稳。\n" +
                                "不是越低越好。不同年龄、病程和合并疾病的人，控制目标可能不同。过度追求低数值也可能增加低血糖风险。\n" +
                                "需要定期复查。复查频率应听从医生建议，不建议自己仅凭感觉判断。\n" +
                                "把它和日常记录结合，才能更完整地理解控糖效果。"),
                article("science", "胰岛素抵抗可以怎样理解", "用通俗方式认识身体对胰岛素反应变弱的状态。", "science-insulin.svg", 83, 34, false,
                        "胰岛素可以帮助血糖进入细胞被利用。胰岛素抵抗可以简单理解为身体对胰岛素的反应变弱，同样的信号效果不如以前。\n" +
                                "它和体重、活动量有关。腹型肥胖、长期缺乏运动、睡眠不足等，都可能加重胰岛素抵抗。\n" +
                                "改善从生活方式开始。规律运动、控制总热量、增加膳食纤维、减少含糖饮料，有助于改善代谢状态。\n" +
                                "不要自行诊断。是否存在胰岛素抵抗，需要结合检查和医生判断。\n" +
                                "理解概念的意义，是帮助自己更愿意坚持长期管理。"),
                article("science", "家族史人群要更早关注什么", "有糖尿病家族史时，更应重视体检和生活方式。", "science-family.svg", 89, 35, false,
                        "家族史会增加糖尿病风险，但并不意味着一定会患病。它更像一个提醒：需要比别人更早关注生活方式和体检指标。\n" +
                                "定期体检别拖延。空腹血糖、餐后血糖、糖化血红蛋白、血脂和血压都值得关注。\n" +
                                "体重和腰围要管理。尤其是腰腹部脂肪增加时，代谢风险可能上升。饮食和运动要尽早建立习惯。\n" +
                                "家庭一起改变更容易。全家少喝甜饮、饭后散步、规律作息，比单独坚持更轻松。\n" +
                                "如果指标异常，应及时线下就医，不要只靠网上信息判断。"),
                article("complication", "为什么要关注眼底检查", "长期血糖异常可能影响眼部微血管，早筛很重要。", "complication-eye.svg", 122, 41, true,
                        "糖尿病相关眼部问题早期可能没有明显感觉，但一旦影响视力，生活质量会受到很大影响。因此定期眼底检查非常重要。\n" +
                                "不要等看不清才检查。视力变化出现时，问题可能已经持续一段时间。按医生建议进行眼底筛查，有助于早发现。\n" +
                                "血糖、血压、血脂一起管。眼部健康不只和血糖有关，高血压和血脂异常也会增加风险。\n" +
                                "留意异常信号。如果出现视物模糊、黑影、闪光感等，应尽快就医。\n" +
                                "预防并发症的重点，是长期稳定管理和定期复查。"),
                article("complication", "足部护理每天要看什么", "检查皮肤、鞋袜和伤口，减少足部问题风险。", "complication-foot.svg", 110, 42, true,
                        "足部护理是糖尿病管理中常被忽视的一环。长期血糖异常可能影响神经和血管，足部小伤口也需要认真对待。\n" +
                                "每天看一看。洗脚后检查脚底、脚趾缝和足跟，看看有没有破损、水泡、红肿或颜色变化。看不到脚底时可借助镜子或家人帮助。\n" +
                                "鞋袜要合适。选择舒适透气、不过紧的鞋袜，新鞋不要一次穿太久。避免赤脚走路。\n" +
                                "处理伤口要谨慎。不要自行修剪很深的鸡眼或老茧。若伤口不愈合、红肿疼痛或渗液，应及时就医。\n" +
                                "足部护理不复杂，关键在每天坚持。"),
                article("complication", "肾脏健康和控糖有什么关系", "关注尿检和肾功能，早期管理更有意义。", "complication-kidney.svg", 82, 43, false,
                        "肾脏负责过滤代谢废物，长期血糖和血压控制不佳可能增加肾脏负担。很多早期肾脏问题并没有明显症状，因此检查很重要。\n" +
                                "关注尿微量白蛋白。它可能帮助发现早期肾脏风险。具体检查频率应按医生建议进行。\n" +
                                "血压也要管。对肾脏来说，血压管理和血糖管理同样重要。不要只盯着一个指标。\n" +
                                "不要乱用药。止痛药、偏方或保健品都可能给肾脏带来额外负担，用药应咨询医生。\n" +
                                "保护肾脏不是单一动作，而是长期综合管理。"),
                article("complication", "心血管风险为什么要一起管理", "血糖、血压、血脂和体重常常相互影响。", "complication-heart.svg", 96, 44, false,
                        "糖尿病风险管理不能只看血糖。血压、血脂、体重、吸烟和运动习惯，都会影响心血管健康。\n" +
                                "定期测量血压和血脂。即使没有明显不舒服，也可能存在风险。体检结果要保存，方便复诊时比较趋势。\n" +
                                "饮食少油少盐。减少油炸食品、肥肉和高盐加工食品，增加蔬菜、全谷物和优质蛋白。\n" +
                                "运动循序渐进。规律有氧和适度力量训练都有帮助，但已有心脏病的人要先听医生建议。\n" +
                                "如果胸闷、胸痛、气短明显，应及时线下就医。"),
                article("complication", "血压偏高时控糖要注意什么", "高血压和血糖异常常同时出现，需要共同管理。", "complication-pressure.svg", 78, 45, false,
                        "血压偏高会增加心、脑、肾等器官负担。对糖尿病或高风险人群来说，血压管理是整体健康管理的一部分。\n" +
                                "家庭测压要规范。固定时间、安静休息后测量，记录日期和数值。不要只凭一次高值就恐慌，也不要连续高值仍忽视。\n" +
                                "饮食减少盐分。少吃腌制品、浓汤、加工肉类和重口味外卖。调味可以用葱姜蒜、醋和香料增加风味。\n" +
                                "体重和运动也关键。适度减重、规律活动和充足睡眠都有助于血压管理。\n" +
                                "是否需要用药、目标是多少，应由医生结合个人情况决定。"),
                article("mistake", "不吃主食真的更控糖吗", "过度限制主食难以长期坚持，也可能造成营养不均衡。", "mistake-rice.svg", 138, 51, true,
                        "很多人把主食看成血糖升高的唯一原因，于是选择完全不吃。短期看似有效，长期却可能带来饥饿、暴食和营养不均衡。\n" +
                                "主食不是敌人。身体需要碳水化合物作为能量来源，关键是选择种类、控制分量，并搭配蛋白质和蔬菜。\n" +
                                "过度限制容易反弹。长期不吃主食可能让人难以坚持，也可能在某一餐大量补偿，反而导致波动。\n" +
                                "更推荐替换和记录。把精白主食部分换成全谷物或杂豆，观察餐后血糖反应，再逐步调整。\n" +
                                "如需严格控制碳水，应在医生或营养师指导下进行。"),
                article("mistake", "无糖食品可以放心多吃吗", "无糖不等于无热量，也不代表适合无限量食用。", "mistake-drink.svg", 126, 52, true,
                        "市面上很多食品标注无糖，但无糖并不等于随便吃。它可能仍含淀粉、脂肪或较高热量，也可能影响体重管理。\n" +
                                "先看营养成分表。关注碳水化合物、能量、脂肪和钠，而不是只看包装正面的宣传语。\n" +
                                "代糖也要适量。多数代糖在合理范围内可作为减少糖摄入的工具，但不建议因此保持嗜甜习惯。\n" +
                                "无糖点心仍是点心。饼干、蛋糕类即使无糖，也可能含较多油脂和精制淀粉。\n" +
                                "真正可靠的方式，仍然是整体饮食结构更均衡。"),
                article("mistake", "运动越猛降糖越快吗", "高强度运动不一定适合所有人，安全更重要。", "mistake-sleep.svg", 90, 53, false,
                        "运动有助于血糖管理，但并不是越猛越好。突然进行高强度运动，可能带来低血糖、关节损伤或心血管风险。\n" +
                                "从基础活动开始。长期不运动的人，先从步行、拉伸和轻阻力训练做起，比直接跑步更稳妥。\n" +
                                "关注身体信号。运动中如果心慌、胸闷、头晕、出冷汗，应立即停止并处理。不要硬撑完成计划。\n" +
                                "规律比爆发重要。每周多次中等强度活动，更适合长期坚持。\n" +
                                "已有并发症或慢病的人，应先和医生确认运动禁忌。"),
                article("mistake", "血糖正常几次就不用管了吗", "偶尔正常不代表风险消失，趋势管理更重要。", "mistake-medicine.svg", 104, 54, false,
                        "看到几次血糖正常，很多人会放松饮食和运动。实际上，血糖受当天饮食、活动、睡眠和药物影响，单次数值不能代表全部。\n" +
                                "看长期趋势。连续记录和糖化血红蛋白，比单次正常更能说明问题。复查计划不要随意取消。\n" +
                                "好习惯要保留。正是因为饮食、运动和作息改善，指标才可能变好。完全恢复旧习惯，风险可能再次上升。\n" +
                                "不要自行停药。正在治疗的人，即使指标改善，也应由医生判断是否调整方案。\n" +
                                "稳定是一种结果，也需要继续维护。"),
                article("mistake", "只靠保健品能控糖吗", "保健品不能替代饮食、运动、监测和正规治疗。", "mistake-diet.svg", 88, 55, false,
                        "一些产品会宣称能快速降糖或替代治疗，这类说法需要警惕。糖尿病和高风险状态的管理，不能建立在单一保健品上。\n" +
                                "不要相信绝对承诺。凡是声称保证治愈、停药不反弹、人人有效的宣传，都不应轻信。\n" +
                                "正规管理有多部分。饮食、运动、体重、睡眠、监测和必要药物，共同构成管理方案。\n" +
                                "注意相互作用。保健品也可能影响肝肾功能或和药物相互作用，使用前应咨询医生。\n" +
                                "把钱和精力放在可验证的生活改变和正规随访上，更稳妥。")
        );
    }

    private SeedArticle article(String category, String title, String summary, String imageName, int viewCount,
                                int sortOrder, boolean recommended, String content) {
        return new SeedArticle(category, title, summary, imageName, viewCount, sortOrder, recommended, content);
    }

    private record SeedArticle(String category, String title, String summary, String imageName,
                               int viewCount, int sortOrder, boolean recommended, String content) {
    }

    private record SeedBanner(String title, String subtitle, String imageName, String linkType, String linkValue,
                              int sortOrder) {
    }

    private record SeedImage(String filename, String startColor, String endColor, String pathOne, String pathTwo,
                             String pathThree) {
    }
}
