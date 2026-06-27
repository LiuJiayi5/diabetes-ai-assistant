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
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ContentSeedInitializer implements ApplicationRunner {

    private static final String STATUS_PUBLISHED = "published";
    private static final String STATUS_OFFLINE = "offline";
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
        List<SeedArticle> seeds = articleSeeds();
        Set<String> seedTitles = seeds.stream().map(SeedArticle::title).collect(Collectors.toSet());
        for (SeedArticle seed : seeds) {
            Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                    .eq(Article::getTitle, seed.title())
                    .last("LIMIT 1"));
            LocalDateTime now = LocalDateTime.now().minusDays(seed.sortOrder() % 20L);
            boolean creating = article == null;
            if (creating) {
                article = new Article();
                article.setCreateTime(now);
            }
            article.setTitle(seed.title());
            article.setCategory(seed.category());
            article.setCoverImage(seedImagePath(seed.imageName()));
            article.setSummary(seed.summary());
            article.setContent(seed.content());
            article.setStatus(STATUS_PUBLISHED);
            article.setViewCount(seed.viewCount());
            article.setIsRecommended(seed.recommended() ? 1 : 0);
            article.setSortOrder(seed.sortOrder());
            article.setUpdateTime(now);
            if (creating) {
                articleMapper.insert(article);
            } else {
                articleMapper.updateById(article);
            }
        }
        repairLegacyArticles(seedTitles);
    }

    private void repairLegacyArticles(Set<String> seedTitles) {
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, STATUS_PUBLISHED));
        for (Article article : articles) {
            if (article == null || seedTitles.contains(article.getTitle())) {
                continue;
            }
            boolean changed = false;
            String category = article.getCategory();
            article.setStatus(STATUS_OFFLINE);
            changed = true;
            if (!hasText(article.getCoverImage())) {
                article.setCoverImage(seedImagePath(defaultImageForCategory(category)));
                changed = true;
            }
            if (!hasText(article.getSummary())) {
                article.setSummary(defaultSummaryForCategory(category));
                changed = true;
            }
            if (!hasText(article.getContent()) || article.getContent().length() < 200) {
                article.setContent(defaultContentForCategory(category));
                changed = true;
            }
            if (article.getViewCount() == null) {
                article.setViewCount(20);
                changed = true;
            }
            if (article.getSortOrder() == null) {
                article.setSortOrder(900 + (article.getArticleId() == null ? 0 : article.getArticleId()));
                changed = true;
            }
            if (changed) {
                article.setUpdateTime(LocalDateTime.now());
                articleMapper.updateById(article);
            }
        }
    }

    private void seedBanners() {
        for (SeedBanner seed : bannerSeeds()) {
            HomeContent existing = homeContentMapper.selectOne(new LambdaQueryWrapper<HomeContent>()
                    .eq(HomeContent::getContentType, TYPE_BANNER)
                    .eq(HomeContent::getTitle, seed.title())
                    .last("LIMIT 1"));
            if (existing != null) {
                existing.setSubtitle(seed.subtitle());
                existing.setImageUrl(seedImagePath(seed.imageName()));
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
            content.setImageUrl(seedImagePath(seed.imageName()));
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
                Files.writeString(target, buildSvg(image), StandardCharsets.UTF_8);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("健康资讯初始化图片生成失败", ex);
        }
    }

    private String seedImagePath(String imageName) {
        return "/uploads/" + SEED_DIR + "/" + imageName;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String defaultImageForCategory(String category) {
        return Map.of(
                "diet", "diet-plate.svg",
                "exercise", "exercise-walk.svg",
                "habit", "habit-sleep.svg",
                "science", "science-meter.svg",
                "complication", "complication-pressure.svg",
                "mistake", "mistake-rice.svg"
        ).getOrDefault(category, "science-meter.svg");
    }

    private String defaultSummaryForCategory(String category) {
        return switch (category == null ? "" : category) {
            case "diet" -> "从餐盘结构、主食份量和清淡烹调入手，帮助日常饮食更稳定。";
            case "exercise" -> "循序渐进安排运动，关注血糖、补水和身体反应。";
            case "habit" -> "通过睡眠、饮水、记录和情绪管理，建立更稳的控糖节奏。";
            case "complication" -> "定期复查相关指标，尽早发现并管理并发症风险。";
            case "mistake" -> "识别常见控糖误区，把注意力放回科学、可持续的生活方式。";
            default -> "用通俗方式理解糖尿病相关知识，帮助日常健康管理更清晰。";
        };
    }

    private String defaultContentForCategory(String category) {
        String label = switch (category == null ? "" : category) {
            case "diet" -> "饮食管理";
            case "exercise" -> "运动管理";
            case "habit" -> "日常习惯";
            case "complication" -> "并发症预防";
            case "mistake" -> "控糖误区";
            default -> "糖尿病科普";
        };
        return label + "不是一次性完成的任务，而是每天都能做一点的小调整。建议先观察自己的饮食、活动、睡眠和血糖记录，再选择最容易坚持的改变，例如减少含糖饮料、餐后轻度活动、固定作息或按时复查。调整过程中不要追求过快结果，也不要因为短期波动就否定全部努力。若血糖明显异常、身体不适或需要调整用药，应及时咨询线下医生。";
    }

    private List<SeedImage> imageSeeds() {
        return List.of(
                image("diet-plate.svg", "#BCEBCF", "#C7E8F7", "#5BBF8A", "plate"),
                image("diet-grains.svg", "#DDF7E9", "#F7E9CC", "#8BBE6A", "grain"),
                image("diet-fruit.svg", "#EAF8F4", "#BEEAF2", "#E8A840", "fruit"),
                image("diet-dining.svg", "#CFEFD9", "#EAF5FA", "#4FAAC4", "dish"),
                image("diet-snack.svg", "#F6F9ED", "#DDF7E9", "#B8862A", "bowl"),
                image("exercise-walk.svg", "#BFE3F5", "#BCEBCF", "#4FAAC4", "walk"),
                image("exercise-week.svg", "#DDF7E9", "#C7E8F7", "#5BBF8A", "stretch"),
                image("exercise-office.svg", "#EAF5FA", "#DDF7E9", "#7A9BD4", "steps"),
                image("exercise-shoes.svg", "#E4F3FB", "#FEF3E2", "#4FAAC4", "shoe"),
                image("exercise-elder.svg", "#F5FBF8", "#BEEAF2", "#5BBF8A", "taiChi"),
                image("habit-sleep.svg", "#EDE8FC", "#D8EEF9", "#7A6DB8", "moon"),
                image("habit-water.svg", "#D8EEF9", "#DDF7E9", "#4FAAC4", "water"),
                image("habit-emotion.svg", "#F3EEFF", "#EAF8F4", "#9B8FD4", "breath"),
                image("habit-record.svg", "#E8F7EE", "#E2F3FA", "#5BBF8A", "record"),
                image("habit-clean.svg", "#EAF5FA", "#DDF7E9", "#7A9BD4", "shield"),
                image("science-meter.svg", "#E5F6EE", "#E4F3FB", "#5BBF8A", "meter"),
                image("science-types.svg", "#EAF8F4", "#BEEAF2", "#4FAAC4", "doctor"),
                image("science-prediabetes.svg", "#F7E9CC", "#DDF7E9", "#E8A840", "report"),
                image("science-glucose.svg", "#CFEFD9", "#EAF5FA", "#5BBF8A", "clock"),
                image("science-bmi.svg", "#E4F3FB", "#DDF7E9", "#7A9BD4", "scale"),
                image("complication-pressure.svg", "#FEF3E2", "#EAF8F4", "#E8A840", "pressure"),
                image("complication-eye.svg", "#FEF8EC", "#BEEAF2", "#4FAAC4", "eye"),
                image("complication-foot.svg", "#F7E9CC", "#E4F3FB", "#B8862A", "foot"),
                image("complication-kidney.svg", "#EAF8F4", "#DDF7E9", "#5BBF8A", "kidney"),
                image("complication-skin.svg", "#F3EEFF", "#EAF8F4", "#9B8FD4", "care"),
                image("mistake-rice.svg", "#E8F4FA", "#F8EADA", "#B8862A", "rice"),
                image("mistake-label.svg", "#EEF2FE", "#DDF7E9", "#7A9BD4", "label"),
                image("mistake-supplement.svg", "#EAF5FA", "#DDF7E9", "#5BBF8A", "capsule"),
                image("mistake-medicine.svg", "#EDE8FC", "#BEEAF2", "#7A6DB8", "medicine"),
                image("mistake-exercise.svg", "#F3EEFF", "#EAF5FA", "#E87878", "pulse"),
                image("banner-prevention.svg", "#AEE8C7", "#BFE9F2", "#5BBF8A", "shield"),
                image("banner-diet.svg", "#BCEBCF", "#DDF7E9", "#8BBE6A", "plate"),
                image("banner-ai.svg", "#BFE3F5", "#EAF8F4", "#4FAAC4", "meter")
        );
    }

    private SeedImage image(String filename, String startColor, String endColor, String accentColor, String symbol) {
        return new SeedImage(filename, startColor, endColor, accentColor, symbol);
    }

    private String buildSvg(SeedImage image) {
        String symbol = switch (image.symbol()) {
            case "grain" -> """
                    <path d="M214 248c8-42 28-76 62-102M250 222c34-4 62-18 86-44M232 188c-28-4-54-18-78-42" fill="none" stroke="%3$s" stroke-width="14" stroke-linecap="round"/>
                    <circle cx="314" cy="176" r="24" fill="#FFFFFF" opacity=".78"/>
                    """;
            case "fruit" -> """
                    <circle cx="236" cy="196" r="54" fill="#FFFFFF" opacity=".82"/>
                    <circle cx="304" cy="206" r="46" fill="%3$s" opacity=".72"/>
                    <path d="M280 138c18-24 42-34 72-30" fill="none" stroke="#4FB783" stroke-width="12" stroke-linecap="round"/>
                    """;
            case "dish", "plate", "bowl", "rice" -> """
                    <ellipse cx="260" cy="218" rx="112" ry="52" fill="#FFFFFF" opacity=".84"/>
                    <path d="M178 218c42 36 122 38 166 0" fill="none" stroke="%3$s" stroke-width="12" stroke-linecap="round"/>
                    <circle cx="226" cy="202" r="17" fill="%3$s" opacity=".86"/>
                    <circle cx="278" cy="198" r="18" fill="#7FC8E8" opacity=".82"/>
                    <circle cx="318" cy="214" r="13" fill="#F4C87A" opacity=".88"/>
                    """;
            case "walk", "steps", "taiChi" -> """
                    <circle cx="256" cy="142" r="28" fill="#FFFFFF" opacity=".86"/>
                    <path d="M248 176l-34 52M262 178l48 44M244 210l66 66M232 210l-54 58" fill="none" stroke="%3$s" stroke-width="14" stroke-linecap="round"/>
                    """;
            case "stretch" -> """
                    <circle cx="254" cy="142" r="27" fill="#FFFFFF" opacity=".86"/>
                    <path d="M198 194h116M254 170l-34 88M254 170l48 90" fill="none" stroke="%3$s" stroke-width="14" stroke-linecap="round"/>
                    """;
            case "shoe" -> """
                    <path d="M164 230c54 34 126 36 206 14 10 22-2 42-30 48H174c-28-8-38-30-10-62Z" fill="#FFFFFF" opacity=".86"/>
                    <path d="M206 232h96M224 206h56" fill="none" stroke="%3$s" stroke-width="12" stroke-linecap="round"/>
                    """;
            case "moon" -> """
                    <path d="M302 136c-28 14-46 44-46 78 0 32 17 61 42 76-64 0-116-48-116-108 0-40 23-75 60-94 8 20 28 38 60 48Z" fill="#FFFFFF" opacity=".86"/>
                    <circle cx="342" cy="118" r="12" fill="%3$s" opacity=".76"/>
                    """;
            case "water" -> """
                    <path d="M260 108c40 52 72 88 72 132 0 42-31 72-72 72s-72-30-72-72c0-44 32-80 72-132Z" fill="#FFFFFF" opacity=".84"/>
                    <path d="M226 240c8 22 24 34 48 34" fill="none" stroke="%3$s" stroke-width="12" stroke-linecap="round"/>
                    """;
            case "breath" -> """
                    <circle cx="260" cy="204" r="70" fill="#FFFFFF" opacity=".78"/>
                    <path d="M214 202c22 24 70 24 92 0M216 170h.1M304 170h.1M180 260c46 24 114 24 160 0" fill="none" stroke="%3$s" stroke-width="13" stroke-linecap="round"/>
                    """;
            case "record", "report", "label" -> """
                    <rect x="176" y="110" width="168" height="190" rx="26" fill="#FFFFFF" opacity=".84"/>
                    <path d="M210 164h98M210 206h76M210 248l28-24 28 20 44-58" fill="none" stroke="%3$s" stroke-width="12" stroke-linecap="round" stroke-linejoin="round"/>
                    """;
            case "shield" -> """
                    <path d="M260 102l92 34v62c0 58-36 92-92 116-56-24-92-58-92-116v-62l92-34Z" fill="#FFFFFF" opacity=".84"/>
                    <path d="M222 208l30 30 58-72" fill="none" stroke="%3$s" stroke-width="15" stroke-linecap="round" stroke-linejoin="round"/>
                    """;
            case "doctor" -> """
                    <circle cx="260" cy="142" r="36" fill="#FFFFFF" opacity=".86"/>
                    <path d="M184 292c18-62 54-92 76-92s58 30 76 92M260 220v52M232 246h56" fill="none" stroke="%3$s" stroke-width="14" stroke-linecap="round"/>
                    """;
            case "meter", "clock", "scale", "pressure" -> """
                    <rect x="166" y="126" width="188" height="154" rx="34" fill="#FFFFFF" opacity=".84"/>
                    <path d="M206 218h30l18-42 30 76 22-34h36" fill="none" stroke="%3$s" stroke-width="12" stroke-linecap="round" stroke-linejoin="round"/>
                    <circle cx="260" cy="164" r="18" fill="%3$s" opacity=".75"/>
                    """;
            case "eye" -> """
                    <path d="M156 206c58-66 150-66 208 0-58 66-150 66-208 0Z" fill="#FFFFFF" opacity=".84"/>
                    <circle cx="260" cy="206" r="36" fill="%3$s" opacity=".76"/>
                    <circle cx="260" cy="206" r="15" fill="#FFFFFF"/>
                    """;
            case "foot" -> """
                    <path d="M206 150c48 4 86 46 96 112 4 26-12 46-40 46h-66c-34 0-52-24-42-54 8-24 24-44 52-104Z" fill="#FFFFFF" opacity=".84"/>
                    <circle cx="298" cy="138" r="11" fill="%3$s" opacity=".84"/>
                    <circle cx="324" cy="154" r="10" fill="%3$s" opacity=".78"/>
                    <circle cx="342" cy="178" r="9" fill="%3$s" opacity=".72"/>
                    """;
            case "kidney" -> """
                    <path d="M222 130c-38 28-48 116-4 152 26 22 58 8 58-26V150c0-24-30-34-54-20ZM298 130c38 28 48 116 4 152-26 22-58 8-58-26V150c0-24 30-34 54-20Z" fill="#FFFFFF" opacity=".84"/>
                    <path d="M260 170v84" stroke="%3$s" stroke-width="12" stroke-linecap="round"/>
                    """;
            case "care" -> """
                    <circle cx="228" cy="186" r="44" fill="#FFFFFF" opacity=".82"/>
                    <rect x="260" y="174" width="90" height="72" rx="18" fill="#FFFFFF" opacity=".82"/>
                    <path d="M282 210h46M305 187v46" stroke="%3$s" stroke-width="12" stroke-linecap="round"/>
                    """;
            case "capsule", "medicine" -> """
                    <rect x="176" y="164" width="172" height="76" rx="38" fill="#FFFFFF" opacity=".84" transform="rotate(-26 262 202)"/>
                    <path d="M260 162l36 72" stroke="%3$s" stroke-width="12" stroke-linecap="round"/>
                    <circle cx="206" cy="262" r="22" fill="%3$s" opacity=".7"/>
                    """;
            case "pulse" -> """
                    <circle cx="260" cy="204" r="82" fill="#FFFFFF" opacity=".78"/>
                    <path d="M170 210h50l20-44 40 98 22-54h48" fill="none" stroke="%3$s" stroke-width="14" stroke-linecap="round" stroke-linejoin="round"/>
                    """;
            default -> """
                    <rect x="176" y="126" width="168" height="168" rx="42" fill="#FFFFFF" opacity=".84"/>
                    <path d="M214 214l32 32 68-82" fill="none" stroke="%3$s" stroke-width="15" stroke-linecap="round" stroke-linejoin="round"/>
                    """;
        };
        return """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 520 340" role="img">
                  <defs>
                    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0" stop-color="%1$s"/>
                      <stop offset="1" stop-color="%2$s"/>
                    </linearGradient>
                    <filter id="shadow" x="-20%%" y="-20%%" width="140%%" height="140%%">
                      <feDropShadow dx="0" dy="14" stdDeviation="18" flood-color="#6FAF96" flood-opacity="0.16"/>
                    </filter>
                  </defs>
                  <rect width="520" height="340" rx="34" fill="url(#bg)"/>
                  <circle cx="420" cy="72" r="88" fill="#FFFFFF" opacity="0.22"/>
                  <circle cx="86" cy="292" r="78" fill="#FFFFFF" opacity="0.18"/>
                  <g filter="url(#shadow)">
                    %4$s
                  </g>
                  <circle cx="392" cy="254" r="16" fill="#FFFFFF" opacity="0.62"/>
                  <circle cx="126" cy="92" r="10" fill="#FFFFFF" opacity="0.55"/>
                </svg>
                """.formatted(image.startColor(), image.endColor(), image.accentColor(), symbol.formatted(image.startColor(), image.endColor(), image.accentColor()));
    }

    private List<SeedBanner> bannerSeeds() {
        return List.of(
                new SeedBanner("从今天开始稳稳控糖", "饮食、运动和监测一起做，逐步建立适合自己的健康节奏。", "banner-prevention.svg", "none", "", 1),
                new SeedBanner("低糖餐盘也可以很好吃", "学会搭配全谷物、蔬菜和优质蛋白，让每一餐更安心。", "banner-diet.svg", "none", "", 2),
                new SeedBanner("AI 助手陪你记录健康变化", "结合健康档案和生活方案，及时发现需要关注的指标。", "banner-ai.svg", "life_plan", "", 3)
        );
    }

    private List<SeedArticle> articleSeeds() {
        return List.of(
                article("diet", "控糖餐盘怎么搭配：一半蔬菜、适量主食和优质蛋白", "用餐盘法安排蔬菜、主食和蛋白质，帮助日常饮食更清晰。", "diet-plate.svg", 168, 101, true,
                        """
                        控糖饮食不等于把餐桌变得很单调。可以先用餐盘法做参照：半盘放非淀粉类蔬菜，如绿叶菜、番茄、黄瓜、菌菇；四分之一放主食，优先选择糙米、燕麦、玉米、全麦面等；另四分之一放鱼、禽、蛋、奶、豆制品或瘦肉。

                        烹调方式尽量清淡，少用糖醋、红烧浓汁和油炸。先吃蔬菜和蛋白质，再吃主食，部分人会感觉餐后波动更平稳。每个人血糖反应不同，建议结合餐后记录调整份量。如血糖明显异常或需要调整用药，应及时咨询线下医生。
                        """),
                article("diet", "主食不是不能吃：糖友如何选择全谷物和杂豆", "主食要定量也要会选，粗细搭配比完全不吃更可持续。", "diet-grains.svg", 146, 102, true,
                        """
                        很多人担心主食升糖，就想完全不吃米饭和面食。实际上，长期不吃主食容易饥饿、乏力，也可能造成下一餐补偿性多吃。更稳妥的做法是控制总量，并把一部分精白主食换成全谷物和杂豆。

                        可以从三分之一替换开始，例如白米中加入糙米、燕麦米、红豆、鹰嘴豆，或把白面包换成全麦面包。杂粮也含碳水，不能无限量吃。搭配蔬菜和优质蛋白，比单独吃一大碗杂粮饭更合适。若肠胃或肾脏情况特殊，调整前应咨询医生。
                        """),
                article("diet", "水果可以吃，但要会选时间和份量", "水果不是禁区，关键是种类、份量、时间和个人血糖反应。", "diet-fruit.svg", 139, 103, false,
                        """
                        糖尿病或高风险人群并不是完全不能吃水果。更推荐选择苹果、梨、柚子、莓果、猕猴桃等相对清爽的水果，控制在小份量，并观察餐后或加餐后的血糖反应。

                        水果最好放在两餐之间，而不是饭后马上大量吃。果汁、果干、蜜饯虽然看起来和水果有关，但糖分更集中，也更容易吃过量，不建议作为日常替代。吃水果当天，主食和点心也要统筹考虑。如果吃某种水果后血糖反复升高，可以减少份量或换种类，并向医生或营养师咨询。
                        """),
                article("diet", "外出吃饭也能控糖：点餐时注意这几点", "外食时从烹调方式、饮料和主食份量入手，降低餐后波动。", "diet-dining.svg", 132, 104, false,
                        """
                        外出吃饭常见问题是油盐多、份量大、饮料甜。点餐时可以优先选择清蒸、炖煮、白灼、少油快炒，少选油炸、糖醋、干锅和浓酱菜。汤汁和蘸料不必全部拌进饭里。

                        饮料尽量选白水或无糖茶。主食可以先盛半份，搭配蔬菜和蛋白质后再看饱腹感。多人聚餐时，不必每道菜都尝很多，先保证自己能执行的部分。若当天饮食明显超出平时，建议按医嘱监测血糖，身体不适时及时线下就医。
                        """),
                article("diet", "加餐不是零食自由：低糖加餐怎么选", "加餐是为了稳定节奏，不是给甜点、奶茶和饼干开绿灯。", "diet-snack.svg", 121, 105, false,
                        """
                        加餐的目的通常是避免两餐间过度饥饿，或配合运动、用药安排，并不代表零食可以随便吃。更适合的选择包括无糖酸奶、少量原味坚果、一个小水果、鸡蛋或少量全麦食品。

                        奶茶、甜点、夹心饼干、膨化食品容易带来糖和热量超标，即使标注“低糖”也要看配料表。加餐应算进全天总量，份量越简单越容易坚持。若经常需要加餐才能避免低血糖，应记录发生时间和症状，并咨询医生评估用药或进餐安排。
                        """),
                article("exercise", "餐后散步为什么适合多数控糖人群", "餐后轻中等强度活动有助于改善餐后血糖波动，但要循序渐进。", "exercise-walk.svg", 174, 201, true,
                        """
                        餐后散步简单、安全门槛低，适合多数需要控糖的人作为日常活动。进餐后不要马上剧烈运动，可以休息片刻，再进行10到20分钟轻中等强度步行，以微微出汗、能说话为宜。

                        散步的重点是规律，而不是速度越快越好。路线尽量平坦，穿舒适鞋袜，天气太热或太冷时可选择室内走动。若正在使用可能导致低血糖的药物，运动前后更要关注身体反应。出现心慌、头晕、胸闷或明显不适，应停止活动并及时咨询医生。
                        """),
                article("exercise", "每周运动怎么安排：有氧、抗阻和拉伸都要有", "把快走、骑车、弹力带和拉伸组合起来，比单一运动更全面。", "exercise-week.svg", 158, 202, true,
                        """
                        控糖运动不只是一味快走。有氧运动能帮助提高能量消耗，抗阻训练有助于保持肌肉量，拉伸则能改善柔韧性和放松状态。三类活动搭配，往往更适合长期坚持。

                        可以每周安排多次快走、骑车或游泳，再加入2到3次弹力带、靠墙俯卧撑、坐姿抬腿等轻抗阻训练。运动前后做简单拉伸，避免突然开始和突然停止。强度从低到中等逐步增加。若有关节、心血管或其他慢病问题，应先听取医生建议。
                        """),
                article("exercise", "久坐人群的控糖运动：从每天多走十分钟开始", "减少久坐比一次性高强度训练更现实，先把活动嵌进日常。", "exercise-office.svg", 142, 203, false,
                        """
                        长时间坐着会让身体代谢变慢，也容易增加体重管理压力。对久坐人群来说，不必一开始就制定很复杂的运动计划，先让每天多动十分钟，就是值得坚持的改变。

                        可以每坐45到60分钟起身活动几分钟，倒水、走楼梯、原地踏步或做肩颈拉伸。上下班提前一站下车、饭后绕小区走一圈，也能累积活动量。目标是减少连续久坐，而不是周末一次运动补回来。若运动后血糖或身体反应异常，应记录并咨询医生。
                        """),
                article("exercise", "运动前后要注意什么：血糖、补水和鞋袜", "运动安全要关注低血糖风险、补水、舒适鞋袜和足部保护。", "exercise-shoes.svg", 137, 204, false,
                        """
                        运动对控糖有帮助，但安全准备同样重要。运动前要了解自己的状态，若空腹、身体不适或血糖波动明显，不宜勉强运动。正在用降糖药或胰岛素的人，更要关注低血糖风险。

                        运动时适量补水，不要等到明显口渴才喝。鞋袜要舒适透气，避免磨脚，运动后检查足部有没有红肿、水泡或破损。随身带一点应急食物，对容易低血糖的人更稳妥。出现胸闷、头晕、出冷汗等情况，应停止运动并及时处理。
                        """),
                article("exercise", "高龄或有慢病的人，运动强度要更保守", "年龄较大或合并慢病时，低强度、可持续和安全评估更重要。", "exercise-elder.svg", 126, 205, false,
                        """
                        高龄人群或合并高血压、冠心病、关节疾病的人，运动目标应更重视安全。适合别人的跑步、爬山或高强度训练，不一定适合自己。先从慢走、太极、八段锦、坐姿训练等低强度活动开始更稳妥。

                        运动时以呼吸平稳、能交流为宜，不追求大汗淋漓。场地要防滑，最好避开清晨过冷或饭后立刻运动。若近期出现胸痛、气短、头晕、下肢疼痛等情况，应先暂停并就医评估。任何运动计划都应根据个人病情调整。
                        """),
                article("habit", "规律作息对血糖管理有什么帮助", "睡眠、夜宵和压力都会影响血糖，稳定作息是基础管理。", "habit-sleep.svg", 151, 301, true,
                        """
                        血糖不只受吃什么影响，睡眠和作息也很关键。熬夜、睡眠不足、夜宵频繁，可能让第二天精神状态和血糖波动都变差。规律作息能帮助身体形成更稳定的代谢节奏。

                        建议尽量固定睡觉和起床时间，睡前减少浓茶、咖啡和长时间刷手机。晚餐不宜过晚，夜间饥饿时也不要随意吃高糖零食。若长期失眠、打鼾明显或晨起疲惫，应重视睡眠问题。血糖持续异常时，要结合医生建议进行评估。
                        """),
                article("habit", "喝水、饮料和控糖：最推荐的其实是白水", "白水和无糖茶更适合日常，含糖饮料容易被忽视。", "habit-water.svg", 138, 302, true,
                        """
                        饮料是很多人控糖时容易忽略的来源。一瓶甜饮、果汁或奶茶，可能在不知不觉中带来较多糖分和能量。日常最推荐的仍然是白水，也可以选择不加糖的淡茶。

                        运动、天气炎热或出汗较多时，要适量补水。所谓“鲜榨果汁”也不等于可以多喝，因为水果被榨汁后更容易摄入过量。无糖饮料偶尔选择可以，但不建议因此保留嗜甜习惯。若出现明显口渴、多尿或血糖升高，应及时监测并咨询医生。
                        """),
                article("habit", "情绪压力也会影响控糖：给自己一点缓冲", "压力会影响饮食、睡眠和活动，学会缓冲比硬扛更重要。", "habit-emotion.svg", 119, 303, false,
                        """
                        情绪紧张时，很多人会睡不好、吃得乱、减少运动，血糖管理也更容易被打乱。压力本身也可能影响身体激素水平，使血糖波动更明显。因此，控糖也需要照顾情绪。

                        可以从小方法开始：每天做几分钟深呼吸，饭后散步，和家人朋友说说压力，给自己留出固定放松时间。不要因为一次指标不好就过度自责，记录原因并调整下一步更有帮助。若长期焦虑、低落或影响生活，应及时寻求专业帮助。
                        """),
                article("habit", "记录血糖和饮食，能帮你发现自己的规律", "把血糖、饮食、运动和睡眠放在一起看，才能找到个人反馈。", "habit-record.svg", 162, 304, false,
                        """
                        同样一餐饭，不同人的血糖反应可能不同。记录能帮助你发现自己的规律，而不是只凭感觉判断。可以记录空腹血糖、餐后血糖、当天主食量、运动、睡眠和特殊情况。

                        记录不需要很复杂，手机备忘录、健康App或纸本都可以。重点是连续、真实，并在复诊时提供给医生参考。看到波动时先回顾原因，例如是否熬夜、外食、运动减少。不要因为某一次数值就自行调整药物，具体治疗变化应听医生建议。
                        """),
                article("habit", "戒烟限酒和控糖：容易被忽视的生活细节", "烟酒会增加心血管和并发症风险，减少暴露也是控糖管理的一部分。", "habit-clean.svg", 117, 305, false,
                        """
                        戒烟限酒常被放在控糖建议的后面，却非常重要。吸烟会增加心血管风险，也可能影响血管和伤口恢复。饮酒则可能带来热量增加、饮食失控或低血糖风险，尤其在空腹饮酒时更需谨慎。

                        如果一时无法完全戒烟，可以先设定减少目标，避开诱因，并寻求家人或专业门诊帮助。饮酒人群应主动和医生沟通是否适合饮酒及安全范围。把烟酒管理和饮食、运动一起做，长期收益更明显。出现身体不适应及时就医。
                        """),
                article("science", "什么是糖尿病：先理解血糖为什么会升高", "用通俗方式理解血糖、胰岛素和长期高血糖风险。", "science-meter.svg", 183, 401, true,
                        """
                        血糖是血液中的葡萄糖，来自食物消化吸收，也来自身体自身调节。胰岛素像一把钥匙，帮助葡萄糖进入细胞被利用。当胰岛素不足，或身体对胰岛素反应变弱时，血糖就可能持续升高。

                        糖尿病并不可怕到无法管理，但也不能忽视。长期高血糖可能影响眼、肾、神经、心血管等多个方面。早发现、早管理，能帮助降低风险。是否患病、属于哪种类型、是否需要用药，都需要结合检查结果和医生判断，不能只靠症状自行诊断。
                        """),
                article("science", "1型、2型和妊娠糖尿病有什么区别", "不同类型的原因和管理重点不同，不能靠感觉自行判断。", "science-types.svg", 141, 402, true,
                        """
                        糖尿病并不是单一类型。1型糖尿病多与胰岛素分泌严重不足有关，通常需要胰岛素治疗。2型糖尿病更常见，常与胰岛素抵抗、体重、年龄、遗传和生活方式有关。妊娠糖尿病则发生在妊娠期间，需要同时关注母婴安全。

                        不同类型的治疗和随访重点不同，不能只凭年龄、胖瘦或症状判断。即使血糖暂时不高，也要按医生建议复查。了解分类的意义，是帮助自己配合检查和管理，而不是自行给自己下结论或随意用药。
                        """),
                article("science", "糖尿病前期是什么意思：它不是小事，也不是绝望", "糖尿病前期提示风险升高，但生活方式干预仍有重要价值。", "science-prediabetes.svg", 136, 403, false,
                        """
                        糖尿病前期是指血糖已经高于理想水平，但尚未达到糖尿病诊断标准的状态。它不是“小问题不用管”，也不是“已经没办法”。这个阶段及时行动，往往能帮助降低进一步发展的风险。

                        管理重点包括体重控制、规律运动、减少含糖饮料、调整主食结构、改善睡眠和定期复查。不要因为没有明显症状就忽视，也不要自行购买药物或保健品。复查项目和频率应听医生建议。把它当成一次提醒，越早建立习惯越有主动权。
                        """),
                article("science", "空腹血糖和餐后血糖，分别看什么", "空腹和餐后指标反映不同侧面，结合趋势更有参考价值。", "science-glucose.svg", 129, 404, false,
                        """
                        空腹血糖通常反映基础状态，容易受前一晚饮食、睡眠、肝糖输出等因素影响。餐后血糖则更能反映一餐饭后的身体反应，和主食种类、份量、进餐速度以及饭后活动有关。

                        有些人空腹血糖不高，但餐后波动明显；也有人清晨数值偏高，需要进一步分析原因。单次测量不能代表全部，连续记录趋势更有意义。检测时间、仪器使用和手部清洁也会影响结果。具体目标范围和处理方式，应根据医生建议确定。
                        """),
                article("science", "BMI、腰围和糖尿病风险有什么关系", "体重和中心型肥胖会影响代谢风险，目标要合理可持续。", "science-bmi.svg", 124, 405, false,
                        """
                        BMI和腰围是评估代谢风险的常用参考。尤其是腰腹部脂肪增加时，身体对胰岛素的反应可能变弱，糖尿病和心血管风险也可能上升。关注体重不是为了追求过瘦，而是为了改善整体健康。

                        减重目标应现实，先减少5%到10%的体重，对部分人就可能带来积极变化。方法上仍是饮食结构、总量控制、规律运动和睡眠管理。不要使用极端节食或来路不明产品。若合并慢病或体重变化异常，应咨询医生。
                        """),
                article("complication", "为什么糖尿病管理不只看血糖，还要看血压和血脂", "心血管风险需要综合管理，血糖、血压、血脂都值得关注。", "complication-pressure.svg", 157, 501, true,
                        """
                        糖尿病管理不是只盯着血糖。血压、血脂、体重、吸烟情况和家族史都会影响心血管风险。即使血糖控制还不错，如果血压和血脂长期异常，也可能增加并发症风险。

                        建议定期复查血压、血脂、肾功能等指标，把结果保存下来，复诊时便于医生判断趋势。日常饮食少油少盐，减少加工食品，保持规律活动。是否需要用药、目标设定多少，都要结合个人情况。出现胸痛、气短等症状应及时就医。
                        """),
                article("complication", "眼部检查不能忽视：视网膜健康需要定期关注", "眼底问题早期可能没有感觉，定期眼科随访很重要。", "complication-eye.svg", 148, 502, true,
                        """
                        糖尿病相关眼部问题早期可能没有明显症状，等到视物模糊、黑影或视力下降时，问题可能已经持续了一段时间。因此，定期眼底检查是预防和早发现的重要环节。

                        眼部健康也受血压、血脂和血糖波动影响，不能只靠滴眼药水或休息解决。若出现视物变形、突然看不清、闪光感等情况，应尽快就医。平时按医嘱复查，保持综合指标稳定，比等症状出现后再处理更稳妥。
                        """),
                article("complication", "糖尿病足如何预防：每天看看脚很重要", "足部检查、鞋袜选择和伤口处理，是每天都能做的预防。", "complication-foot.svg", 143, 503, false,
                        """
                        长期血糖异常可能影响神经和血管，足部小伤口也需要认真对待。每天洗脚后看一看脚底、脚趾缝、足跟和指甲周围，是否有红肿、水泡、破损、颜色变化或疼痛。

                        鞋袜要合脚、透气，不要赤脚走路，新鞋不要一次穿太久。修剪指甲时避免剪得过深。若有鸡眼、老茧或伤口，不建议自行用刀片处理。伤口不愈合、渗液、红肿或疼痛加重时，应及时到医院处理。
                        """),
                article("complication", "肾脏健康与糖尿病：早期筛查更安心", "尿检、肾功能和血压管理能帮助更早发现肾脏风险。", "complication-kidney.svg", 126, 504, false,
                        """
                        肾脏负责过滤代谢废物，长期血糖和血压控制不佳会增加肾脏负担。早期肾脏问题可能没有明显不适，因此不能只凭感觉判断是否安全。

                        定期检查尿微量白蛋白、肾功能和血压，有助于早发现风险。日常饮食注意少盐，避免随意使用止痛药、偏方或不明保健品，以免增加肾脏负担。若检查提示异常，应按医生建议复查和治疗。保护肾脏靠长期综合管理，不是临时补救。
                        """),
                article("complication", "口腔和皮肤也要管：小问题别拖成大麻烦", "牙龈、皮肤感染和小伤口都值得关注，清洁和就医要及时。", "complication-skin.svg", 118, 505, false,
                        """
                        血糖长期不稳定时，口腔和皮肤问题也可能增多。牙龈出血、口腔异味、皮肤瘙痒、反复感染或小伤口愈合慢，都不应长期拖着。

                        日常要坚持刷牙、使用牙线或冲牙器，定期口腔检查。皮肤保持清洁和干燥，避免抓挠导致破损。脚部、腹股沟等容易潮湿的部位更要留意。若出现红肿、化脓、发热或伤口不愈合，应及时就医，不要自行乱用药膏。
                        """),
                article("mistake", "误区一：不吃主食就能控糖吗", "主食不能完全取消，选择种类和控制份量更重要。", "mistake-rice.svg", 176, 601, true,
                        """
                        不吃主食看似能让血糖短期下降，但很难长期坚持，也可能带来饥饿、乏力、营养不均衡和暴食反弹。主食不是控糖的敌人，关键在于种类、份量和搭配。

                        建议减少精白米面的大份量摄入，适当加入全谷物、杂豆和薯类。每餐主食不要单独吃，搭配蔬菜和优质蛋白更稳。若需要更严格的碳水控制，应在医生或营养师指导下进行。不要自行采用极端饮食，更不要因此随意调整药物。
                        """),
                article("mistake", "误区二：无糖食品可以随便吃吗", "无糖不等于无热量，也不代表可以不看配料和总量。", "mistake-label.svg", 154, 602, true,
                        """
                        “无糖”常让人放松警惕，但无糖食品不一定低热量，也可能含有较多淀粉、脂肪或钠。饼干、糕点、饮料即使不添加蔗糖，也不等于可以无限量吃。

                        购买时要看营养成分表，关注能量、碳水化合物、脂肪和配料顺序。代糖可以帮助减少糖摄入，但不应成为保持嗜甜习惯的理由。更重要的是把天然食物、规律三餐和总量控制放在前面。吃后血糖波动明显时，应减少或避免。
                        """),
                article("mistake", "误区三：只靠保健品就能降糖吗", "保健品不能替代饮食、运动、监测和正规治疗。", "mistake-supplement.svg", 149, 603, false,
                        """
                        一些产品会宣传“快速降糖”“停药不反弹”，这类说法需要格外谨慎。糖尿病或糖尿病前期管理，不能建立在单一保健品上，更不能用它替代医生制定的治疗方案。

                        正规管理包括饮食、运动、体重、睡眠、血糖监测和必要药物。保健品也可能和药物相互影响，或增加肝肾负担。购买前应看清批准信息和成分，不相信绝对化承诺。若血糖异常，应及时线下就医，而不是拖延治疗。
                        """),
                article("mistake", "误区四：血糖正常了就可以停药吗", "指标改善不等于可以自行停药，治疗调整要和医生沟通。", "mistake-medicine.svg", 133, 604, false,
                        """
                        看到血糖正常几次，有些人会想自行停药。实际上，指标变好可能正是饮食、运动和药物共同作用的结果。突然停药可能让血糖再次升高，甚至带来更大波动。

                        是否减药、停药或换药，需要结合血糖记录、糖化血红蛋白、低血糖风险、并发症和整体健康情况判断。复诊时可以主动把记录给医生看，一起讨论方案。生活方式仍要坚持，不要把“正常一次”理解成风险消失。
                        """),
                article("mistake", "误区五：运动越猛越好吗", "过度运动可能带来低血糖和损伤风险，适合自己才重要。", "mistake-exercise.svg", 128, 605, false,
                        """
                        运动有益控糖，但不是越猛越好。突然进行高强度跑步、长时间爬山或空腹运动，可能导致低血糖、关节损伤或心血管风险，尤其是平时活动少或已有慢病的人。

                        更推荐循序渐进：先从散步、骑车、拉伸和轻抗阻开始，逐步增加时间和强度。运动前后关注血糖、补水和足部情况。出现头晕、心慌、胸闷或出冷汗，应立即停止并处理。运动计划最好结合医生建议制定。
                        """)
        );
    }

    private SeedArticle article(String category, String title, String summary, String imageName, int viewCount,
                                int sortOrder, boolean recommended, String content) {
        return new SeedArticle(category, title, summary, imageName, viewCount, sortOrder, recommended,
                normalizeSeedContent(content));
    }

    private String normalizeSeedContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.replaceAll("\\s+", "").length() >= 200) {
            return normalized;
        }
        return normalized + "\n\n建议先选择一两项最容易执行的做法，坚持记录饮食、活动和血糖变化，再根据反馈逐步调整。若血糖明显异常、身体不适或需要调整用药，应及时咨询线下医生。";
    }

    private record SeedArticle(String category, String title, String summary, String imageName,
                               int viewCount, int sortOrder, boolean recommended, String content) {
    }

    private record SeedBanner(String title, String subtitle, String imageName, String linkType, String linkValue,
                              int sortOrder) {
    }

    private record SeedImage(String filename, String startColor, String endColor, String accentColor, String symbol) {
    }
}
