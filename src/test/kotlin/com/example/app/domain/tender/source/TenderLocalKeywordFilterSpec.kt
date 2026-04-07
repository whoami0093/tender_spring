package com.example.app.domain.tender.source

import com.example.app.domain.tender.subscription.Subscription
import com.example.app.domain.tender.subscription.toFilters
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.time.Instant
import java.util.stream.Stream

/**
 * Spec for TenderFilters.matchesTender — local keyword post-filter.
 *
 * Covers the "хоз товары" use-case: after the API returns results for the search keyword,
 * we apply a secondary filter to keep only tenders whose objectInfo actually contains
 * household-goods related terms.
 */
class TenderLocalKeywordFilterSpec {
    // ── 1. empty localKeywords — everything passes ────────────────────────────

    @Test
    fun `no local keywords — all tenders pass regardless of objectInfo`() {
        val filters = TenderFilters(localKeywords = emptyList())

        val unrelatedTexts =
            listOf(
                "Поставка строительных материалов",
                "Оказание транспортных услуг",
                "Закупка медицинского оборудования",
                "Ремонт кровли административного здания",
                "Поставка продуктов питания для столовой",
                "Услуги охраны объекта",
                "Поставка офисной мебели",
                "IT-оборудование и программное обеспечение",
            )

        unrelatedTexts.forEach { info ->
            assertThat(filters.matchesTender(tender(info)))
                .`as`("tender with «$info» must pass when localKeywords is empty")
                .isTrue()
        }
    }

    // ── 2. хоз товары — matching cases ───────────────────────────────────────

    @ParameterizedTest(name = "[{index}] SHOULD match: \"{0}\"")
    @MethodSource("householdGoodsMatchingTexts")
    fun `хоз товары filter — matching objectInfo texts are kept`(objectInfo: String) {
        val filters = householdGoodsFilters()
        assertThat(filters.matchesTender(tender(objectInfo)))
            .`as`("Expected «$objectInfo» to MATCH хоз товары filter")
            .isTrue()
    }

    // ── 3. хоз товары — non-matching cases ───────────────────────────────────

    @ParameterizedTest(name = "[{index}] SHOULD NOT match: \"{0}\"")
    @MethodSource("nonHouseholdTexts")
    fun `хоз товары filter — unrelated objectInfo texts are dropped`(objectInfo: String) {
        val filters = householdGoodsFilters()
        assertThat(filters.matchesTender(tender(objectInfo)))
            .`as`("Expected «$objectInfo» to NOT match хоз товары filter")
            .isFalse()
    }

    // ── 4. case-insensitivity ─────────────────────────────────────────────────

    @Test
    fun `matching is case-insensitive for both keyword and objectInfo`() {
        val filters = TenderFilters(localKeywords = listOf("Хозяйств"))

        assertThat(filters.matchesTender(tender("поставка ХОЗЯЙСТВЕННЫХ товаров"))).isTrue()
        assertThat(filters.matchesTender(tender("Хозяйственные принадлежности"))).isTrue()
        assertThat(filters.matchesTender(tender("закупка хозяйственного инвентаря"))).isTrue()
    }

    // ── 5. keyword must be substring ──────────────────────────────────────────

    @Test
    fun `partial keyword match works — keyword is substring of objectInfo word`() {
        val filters = TenderFilters(localKeywords = listOf("хоз"))

        assertThat(filters.matchesTender(tender("хозяйственные мешки для мусора"))).isTrue()
        assertThat(filters.matchesTender(tender("хозтовары для офиса"))).isTrue()
        assertThat(filters.matchesTender(tender("хоз. инвентарь (вёдра, щётки)"))).isTrue()
    }

    // ── 6. multiple localKeywords — OR logic ─────────────────────────────────

    @Test
    fun `any single matching keyword from the list is sufficient`() {
        val filters = TenderFilters(localKeywords = listOf("хозяйств", "уборк", "моющ", "чистящ"))

        assertThat(filters.matchesTender(tender("Поставка чистящих средств для школы"))).isTrue()
        assertThat(filters.matchesTender(tender("Услуги по уборке территории"))).isTrue()
        assertThat(filters.matchesTender(tender("Хозяйственный инвентарь (20 наименований)"))).isTrue()
        assertThat(filters.matchesTender(tender("Закупка моющих средств для медучреждения"))).isTrue()
        assertThat(filters.matchesTender(tender("Поставка строительных материалов"))).isFalse()
    }

    // ── 7. empty objectInfo ───────────────────────────────────────────────────

    @Test
    fun `tender with empty objectInfo does not match non-empty localKeywords`() {
        val filters = TenderFilters(localKeywords = listOf("хоз", "уборк"))
        assertThat(filters.matchesTender(tender(""))).isFalse()
    }

    // ── 8. Subscription.toFilters round-trip ─────────────────────────────────

    @Test
    fun `toFilters maps filterLocalKeywords column correctly`() {
        val sub = buildSubscription(filterLocalKeywords = "хозяйств,уборк, чистящ , моющ")
        val filters = sub.toFilters()

        assertThat(filters.localKeywords).containsExactlyInAnyOrder("хозяйств", "уборк", "чистящ", "моющ")
    }

    @Test
    fun `toFilters returns empty localKeywords when column is null`() {
        val sub = buildSubscription(filterLocalKeywords = null)
        assertThat(sub.toFilters().localKeywords).isEmpty()
    }

    @Test
    fun `toFilters returns empty localKeywords when column has only blanks`() {
        val sub = buildSubscription(filterLocalKeywords = "  , ,  ")
        assertThat(sub.toFilters().localKeywords).isEmpty()
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun householdGoodsFilters() =
        TenderFilters(
            keywords = listOf("хоз товары"),
            localKeywords =
                listOf(
                    "хоз",
                    "уборк",
                    "убороч",
                    "клининг",
                    "моющ",
                    "чистящ",
                    "дезинф",
                    "антисепт",
                    "гигиен",
                    "санит",
                    "бытов",
                    "стиральн",
                    "мыл",
                    "перчатк",
                    "салфетк",
                    "швабр",
                    "щётк",
                    "ведр",
                    "тряпк",
                    "губк",
                    "ветошь",
                    "полотенц",
                    "туалетн",
                    "диспенсер",
                    "освежит",
                    "для мусора",
                    "мусорн",
                ),
        )

    private fun tender(objectInfo: String) =
        Tender(
            purchaseNumber = "TEST-001",
            objectInfo = objectInfo,
            customerInn = null,
            maxPrice = BigDecimal("50000"),
            currency = "RUB",
            deadline = null,
            publishedAt = Instant.now(),
            eisUrl = "https://zakupki.gov.ru/test/TEST-001",
            source = "GOSPLAN_44",
        )

    private fun buildSubscription(filterLocalKeywords: String?) =
        Subscription(
            id = 1L,
            source = "GOSPLAN_44",
            emails = "[\"test@example.com\"]",
            filterLocalKeywords = filterLocalKeywords,
        )

    companion object {
        @JvmStatic
        fun householdGoodsMatchingTexts(): Stream<Arguments> =
            Stream.of(
                // хозяйственные / хозтовары
                Arguments.of("Поставка хозяйственных товаров для нужд учреждения"),
                Arguments.of("Хозяйственный инвентарь (швабры, вёдра, щётки) — 50 наим."),
                Arguments.of("Закупка хозтоваров для общежития"),
                Arguments.of("Хоз. принадлежности для кухни и санузлов"),
                Arguments.of("Поставка хозяйственного мыла и бумажных полотенец"),
                Arguments.of("Хозяйственные перчатки латексные, 100 пар"),
                Arguments.of("Инвентарь хозяйственный: вёдра пластиковые 10 л — 20 шт."),
                Arguments.of("хозтовары и расходные материалы для клининга"),
                Arguments.of("Поставка хозяйственно-бытовых товаров по перечню"),
                Arguments.of("Закупка хоз. товаров согласно спецификации"),
                // уборка / клининг
                Arguments.of("Услуги по уборке производственных помещений"),
                Arguments.of("Средства для уборки и дезинфекции помещений"),
                Arguments.of("Закупка инвентаря для уборки территории"),
                Arguments.of("Поставка товаров для клининга и уборки помещений"),
                Arguments.of("Уборочный инвентарь: метёлки, совки, тряпки"),
                Arguments.of("Мешки для мусора и средства для уборки санузлов"),
                // моющие / чистящие
                Arguments.of("Моющие средства для посудомоечных машин — 200 л"),
                Arguments.of("Поставка чистящих и моющих средств для пищеблока"),
                Arguments.of("Чистящий порошок и жидкость для сантехники"),
                Arguments.of("Закупка моющего средства для полов (концентрат)"),
                Arguments.of("Гель чистящий для унитазов, 48 шт"),
                Arguments.of("Средства моющие и дезинфицирующие согласно ГОСТ"),
                // дезинфекция / санитария
                Arguments.of("Дезинфицирующее средство для поверхностей"),
                Arguments.of("Поставка дезинфектантов и антисептиков для медорганизации"),
                Arguments.of("Санитарно-гигиенические средства: мыло жидкое, антисептик"),
                Arguments.of("Дезинфекция помещений и поставка дезсредств"),
                Arguments.of("Закупка санитайзеров и дезинфицирующих салфеток"),
                Arguments.of("Санитарная обработка системы вентиляции"),
                // гигиена
                Arguments.of("Средства личной гигиены для детского лагеря"),
                Arguments.of("Гигиенические перчатки нитриловые, 1000 шт"),
                Arguments.of("Поставка гигиенических пакетов и диспенсеров"),
                Arguments.of("Туалетная бумага, бумажные полотенца, гигиенические прокладки"),
                // мусорные мешки
                Arguments.of("Мешки для мусора 120 л (чёрные) — 500 рулонов"),
                Arguments.of("Мешки мусорные для раздельного сбора отходов"),
                Arguments.of("Пакеты для мусора 30 л и 60 л — годовая потребность"),
                Arguments.of("Закупка мусорных контейнеров и мешков для ТБО"),
                // бытовая химия
                Arguments.of("Бытовая химия и расходные материалы для санузлов"),
                Arguments.of("Поставка бытовой химии по договору поставки"),
                Arguments.of("Средства бытовой химии для уборки и стирки"),
            )

        @JvmStatic
        fun nonHouseholdTexts(): Stream<Arguments> =
            Stream.of(
                // строительство и ремонт
                Arguments.of("Поставка строительных материалов (кирпич, цемент, песок)"),
                Arguments.of("Ремонт и реконструкция административного здания"),
                Arguments.of("Замена кровли и фасадных панелей"),
                Arguments.of("Монтаж системы видеонаблюдения"),
                Arguments.of("Устройство тротуарной плитки на территории объекта"),
                // продукты питания
                Arguments.of("Поставка продуктов питания для детского сада на квартал"),
                Arguments.of("Закупка хлеба и хлебобулочных изделий"),
                Arguments.of("Мясо охлаждённое (говядина, свинина) — 500 кг"),
                Arguments.of("Молочная продукция: молоко, кефир, творог"),
                Arguments.of("Поставка овощей и фруктов для школьной столовой"),
                // медицина и оборудование
                Arguments.of("Поставка медицинского оборудования (томограф МРТ)"),
                Arguments.of("Закупка лекарственных средств по перечню ЖНВЛП"),
                Arguments.of("Одноразовые шприцы и расходный медицинский материал"),
                Arguments.of("Хирургический инструментарий и перевязочные материалы"),
                // IT и оргтехника
                Arguments.of("Поставка ноутбуков и периферийного оборудования"),
                Arguments.of("Лицензии на программное обеспечение Microsoft Office"),
                Arguments.of("Обслуживание серверной инфраструктуры"),
                Arguments.of("Закупка оргтехники: принтеры, МФУ, сканеры"),
                // транспорт и услуги
                Arguments.of("Аренда транспортных средств для нужд организации"),
                Arguments.of("Услуги охранного предприятия"),
                Arguments.of("Техническое обслуживание автопарка"),
                Arguments.of("Перевозка грузов автомобильным транспортом"),
                // прочее
                Arguments.of("Организация корпоративного питания сотрудников"),
                Arguments.of("Образовательные услуги: курсы повышения квалификации"),
                Arguments.of("Канцелярские принадлежности для офиса"),
                Arguments.of("Мебель офисная: столы, кресла, стеллажи"),
                Arguments.of("Типографские услуги: печать брошюр и буклетов"),
                Arguments.of("Поставка спортивного инвентаря для секции"),
                Arguments.of("Озеленение и благоустройство территории"),
            )
    }
}
