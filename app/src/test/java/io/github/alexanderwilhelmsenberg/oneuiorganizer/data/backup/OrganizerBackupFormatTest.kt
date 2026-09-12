package io.github.alexanderwilhelmsenberg.oneuiorganizer.data.backup

import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryNamePolicy
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.backup.OrganizerBackupMapper
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.backup.OrganizerBackupValidator
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CustomCategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupCategoryOverride
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupCustomCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupDocument
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupError
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupFormatVersion
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupOrderProblem
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class OrganizerBackupFormatTest {
    @Test
    fun `format version is independent from organizer state schema`() {
        assertEquals(1, OrganizerBackupFormatVersion.CURRENT.value)
        assertEquals(2, OrganizerState.CURRENT_SCHEMA_VERSION)
        assertNotEquals(OrganizerBackupFormatVersion.CURRENT.value, OrganizerState.CURRENT_SCHEMA_VERSION)
    }

    @Test
    fun `export is deterministic and round trips all user owned state with stable ids`() {
        val customId = CategoryId.custom("stable-category-id")
        val uninstalled = AppId("example.uninstalled")
        val installed = AppId("example.installed")
        val expected =
            OrganizerState(
                categoryOverrides =
                    linkedMapOf(
                        uninstalled to customId,
                        installed to AppCategory.WORK.id
                    ),
                favouriteAppIds = linkedSetOf(uninstalled, installed),
                hiddenAppIds = linkedSetOf(installed, uninstalled),
                customCategories =
                    listOf(
                        CustomCategoryDefinition(
                            id = customId,
                            displayName = "Stable Name"
                        )
                    ),
                categoryOrder = listOf(customId) + OrganizerState.defaultBuiltInCategoryOrder()
            )

        val firstEncoded = OrganizerBackupJsonCodec.encode(OrganizerBackupMapper.fromState(expected))
        val sameStateDifferentCollectionOrder =
            expected.copy(
                categoryOverrides =
                    linkedMapOf(
                        installed to AppCategory.WORK.id,
                        uninstalled to customId
                    ),
                favouriteAppIds = linkedSetOf(installed, uninstalled),
                hiddenAppIds = linkedSetOf(uninstalled, installed)
            )
        val secondEncoded =
            OrganizerBackupJsonCodec.encode(OrganizerBackupMapper.fromState(sameStateDifferentCollectionOrder))

        assertEquals(firstEncoded, secondEncoded)
        assertTrue(firstEncoded.indexOf("example.installed") < firstEncoded.indexOf("example.uninstalled"))
        val decoded = assertSuccess(OrganizerBackupJsonCodec.decode(firstEncoded))
        val prepared = assertSuccess(OrganizerBackupValidator.prepare(decoded))
        assertEquals(expected.normalized(), prepared.organizerState)
        assertEquals(customId, prepared.organizerState.customCategories.single().id)
        assertEquals(customId, prepared.organizerState.categoryOverrides[uninstalled])
    }

    @Test
    fun `malformed document is rejected without leaking parser failure`() {
        assertEquals(
            OrganizerBackupError.MalformedDocument,
            assertFailure(OrganizerBackupJsonCodec.decode("{ definitely-not-json"))
        )
        assertEquals(
            OrganizerBackupError.MalformedDocument,
            assertFailure(OrganizerBackupJsonCodec.decode("{\"formatVersion\":1}"))
        )
    }

    @Test
    fun `unknown fields are ignored for forward compatible v1 reading`() {
        val encoded = OrganizerBackupJsonCodec.encode(validDocument())
        val withUnknownField = encoded.dropLast(1) + ",\"futureMetadata\":{\"ignored\":true}}"

        val decoded = assertSuccess(OrganizerBackupJsonCodec.decode(withUnknownField))

        assertEquals(validDocument(), decoded)
    }

    @Test
    fun `unsupported backup version is rejected by backup version not state schema`() {
        val error =
            assertFailure(
                OrganizerBackupValidator.prepare(
                    validDocument().copy(formatVersion = OrganizerBackupFormatVersion(2))
                )
            )

        assertEquals(OrganizerBackupError.UnsupportedFormatVersion(2), error)
    }

    @Test
    fun `duplicate and invalid custom identities are rejected`() {
        val valid = validDocument()
        val custom = valid.customCategories.single()

        assertEquals(
            OrganizerBackupError.DuplicateCustomCategoryId(custom.categoryId),
            assertFailure(
                OrganizerBackupValidator.prepare(
                    valid.copy(customCategories = listOf(custom, custom.copy(displayName = "Other")))
                )
            )
        )
        assertEquals(
            OrganizerBackupError.InvalidCustomCategoryId(AppCategory.WORK.id),
            assertFailure(
                OrganizerBackupValidator.prepare(
                    valid.copy(
                        customCategories =
                            listOf(
                                OrganizerBackupCustomCategory(
                                    categoryId = AppCategory.WORK.id,
                                    displayName = "Portable"
                                )
                            )
                    )
                )
            )
        )
    }

    @Test
    fun `custom names reuse category management name invariants`() {
        val valid = validDocument()
        val customId = valid.customCategories.single().categoryId
        val tooLong = "x".repeat(CategoryNamePolicy.MAX_CODE_POINTS + 1)

        assertEquals(
            OrganizerBackupError.InvalidCustomCategoryName(customId),
            assertFailure(
                OrganizerBackupValidator.prepare(
                    valid.copy(
                        customCategories =
                            listOf(
                                OrganizerBackupCustomCategory(
                                    categoryId = customId,
                                    displayName = tooLong
                                )
                            )
                    )
                )
            )
        )
        assertEquals(
            OrganizerBackupError.InvalidCustomCategoryName(customId),
            assertFailure(
                OrganizerBackupValidator.prepare(
                    valid.copy(
                        customCategories =
                            listOf(
                                OrganizerBackupCustomCategory(
                                    categoryId = customId,
                                    displayName = "Work"
                                )
                            )
                    )
                )
            )
        )
    }

    @Test
    fun `duplicate app identities and unresolved override are rejected`() {
        val valid = validDocument()
        val override = valid.categoryOverrides.single()
        val missingCategory = CategoryId.custom("missing")

        assertEquals(
            OrganizerBackupError.DuplicateCategoryOverride(override.appId),
            assertFailure(
                OrganizerBackupValidator.prepare(
                    valid.copy(categoryOverrides = listOf(override, override))
                )
            )
        )
        assertEquals(
            OrganizerBackupError.DuplicateFavouriteAppId(valid.favouriteAppIds.single()),
            assertFailure(
                OrganizerBackupValidator.prepare(
                    valid.copy(favouriteAppIds = valid.favouriteAppIds + valid.favouriteAppIds.single())
                )
            )
        )
        assertEquals(
            OrganizerBackupError.DuplicateHiddenAppId(valid.hiddenAppIds.single()),
            assertFailure(
                OrganizerBackupValidator.prepare(
                    valid.copy(hiddenAppIds = valid.hiddenAppIds + valid.hiddenAppIds.single())
                )
            )
        )
        assertEquals(
            OrganizerBackupError.InvalidCategoryReference(missingCategory),
            assertFailure(
                OrganizerBackupValidator.prepare(
                    valid.copy(
                        categoryOverrides =
                            listOf(
                                OrganizerBackupCategoryOverride(
                                    appId = override.appId,
                                    categoryId = missingCategory
                                )
                            )
                    )
                )
            )
        )
    }

    @Test
    fun `category order must be exact duplicate free permutation of imported categories`() {
        val valid = validDocument()
        val first = valid.categoryOrder.first()
        val unknown = CategoryId.custom("unknown-order")

        assertEquals(
            OrganizerBackupError.InvalidCategoryOrder(
                problem = OrganizerBackupOrderProblem.DUPLICATE_ID,
                categoryId = first
            ),
            assertFailure(
                OrganizerBackupValidator.prepare(
                    valid.copy(categoryOrder = valid.categoryOrder + first)
                )
            )
        )
        assertEquals(
            OrganizerBackupError.InvalidCategoryOrder(
                problem = OrganizerBackupOrderProblem.UNKNOWN_ID,
                categoryId = unknown
            ),
            assertFailure(
                OrganizerBackupValidator.prepare(
                    valid.copy(categoryOrder = valid.categoryOrder.dropLast(1) + unknown)
                )
            )
        )
        val missing = valid.categoryOrder.last()
        assertEquals(
            OrganizerBackupError.InvalidCategoryOrder(
                problem = OrganizerBackupOrderProblem.MISSING_ID,
                categoryId = missing
            ),
            assertFailure(
                OrganizerBackupValidator.prepare(
                    valid.copy(categoryOrder = valid.categoryOrder.dropLast(1))
                )
            )
        )
    }

    private fun validDocument(): OrganizerBackupDocument {
        val customId = CategoryId.custom("portable")
        return OrganizerBackupDocument(
            formatVersion = OrganizerBackupFormatVersion.CURRENT,
            categoryOverrides =
                listOf(
                    OrganizerBackupCategoryOverride(
                        appId = AppId("example.uninstalled"),
                        categoryId = customId
                    )
                ),
            favouriteAppIds = listOf(AppId("example.favourite")),
            hiddenAppIds = listOf(AppId("example.hidden")),
            customCategories =
                listOf(
                    OrganizerBackupCustomCategory(
                        categoryId = customId,
                        displayName = "Portable"
                    )
                ),
            categoryOrder = listOf(customId) + OrganizerState.defaultBuiltInCategoryOrder()
        )
    }

    private fun <T> assertSuccess(result: OrganizerBackupResult<T>): T = when (result) {
        is OrganizerBackupResult.Success -> result.value
        is OrganizerBackupResult.Failure -> fail("Expected success but got ${result.error}.")
    }

    private fun assertFailure(result: OrganizerBackupResult<*>): OrganizerBackupError = when (result) {
        is OrganizerBackupResult.Failure -> result.error
        is OrganizerBackupResult.Success -> fail("Expected failure but got ${result.value}.")
    }
}
