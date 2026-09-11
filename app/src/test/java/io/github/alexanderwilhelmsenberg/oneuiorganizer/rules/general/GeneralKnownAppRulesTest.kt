package io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.general

import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.DefaultCategoryEngine
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.ClassificationSource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.PlatformAppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.KnownAppRule
import io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.KnownAppRuleSet
import io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.KnownAppSelector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class GeneralKnownAppRulesTest {
    private val ruleSet = KnownAppRuleSet(GeneralKnownAppRules.entries)

    @Test
    fun evidenceBackedPackagesResolveToExpectedCategories() {
        val expected = expectedRules()

        assertEquals(expected.size, GeneralKnownAppRules.entries.size)
        expected.forEach { (packageName, category) ->
            assertEquals(
                category,
                ruleSet.categoryFor(installedApp(packageName, label = "Misleading localized label"))
            )
        }
    }

    @Test
    fun generalRulePackUsesExactPackageIdentityOnly() {
        GeneralKnownAppRules.entries.forEach { rule ->
            assertIs<KnownAppSelector.ExactPackage>(rule.selector)
        }
    }

    @Test
    fun nearbyPackageIdentityDoesNotMatch() {
        assertNull(ruleSet.categoryFor(installedApp("com.reddit.frontpage.beta")))
        assertNull(ruleSet.categoryFor(installedApp("com.samsung.android.messaging.clone")))
        assertNull(ruleSet.categoryFor(installedApp("org.fdroid.fdroid.debug")))
    }

    @Test
    fun manualOverrideStillWinsOverExpandedGeneralRules() {
        val app =
            installedApp(
                packageName = "com.Slack",
                platformCategory = PlatformAppCategory.PRODUCTIVITY
            )
        val engine = DefaultCategoryEngine(knownAppCategory = ruleSet::categoryFor)

        val automatic = engine.categorize(app, userOverride = null)
        val overridden = engine.categorize(app, userOverride = AppCategory.COMMUNICATION)

        assertEquals(AppCategory.WORK, automatic.category)
        assertEquals(ClassificationSource.KNOWN_APP_RULE, automatic.source)
        assertEquals(AppCategory.COMMUNICATION, overridden.category)
        assertEquals(ClassificationSource.USER_OVERRIDE, overridden.source)
    }

    @Test
    fun duplicateGeneralSelectorIsRejected() {
        val selector = KnownAppSelector.ExactPackage(AppId("com.reddit.frontpage"))

        assertFailsWith<IllegalStateException> {
            KnownAppRuleSet(
                GeneralKnownAppRules.entries +
                    listOf(KnownAppRule(selector, AppCategory.TOOLS))
            )
        }
    }

    private fun expectedRules(): Map<String, AppCategory> =
        buildMap {
            putPackages(
                AppCategory.COMMUNICATION,
                "com.famly.famly",
                "com.samsung.android.app.contacts",
                "com.samsung.android.dialer",
                "com.samsung.android.messaging",
                "com.spond.spond",
                "parent.vigilo.no.parentapplication"
            )
            putPackages(
                AppCategory.SOCIAL,
                "com.laurencedawson.reddit_sync",
                "com.reddit.frontpage",
                "org.cygnusx1.continuum"
            )
            putPackages(
                AppCategory.WORK,
                "com.microsoft.teams",
                "com.Slack"
            )
            putPackages(
                AppCategory.PRODUCTIVITY,
                "com.samsung.android.app.reminder",
                "com.samsung.android.calendar"
            )
            putPackages(
                AppCategory.SMART_HOME,
                "com.aircoookie.WLED",
                "com.airthings.app.android",
                "com.ants360.yicamera.international",
                "com.assaabloy.yale",
                "com.google.android.apps.chromecast.app",
                "com.philips.lighting.hue2",
                "com.roborock.smart",
                "com.samsung.android.oneconnect",
                "com.tibber.android",
                "de.miele.infocontrol",
                "io.homeassistant.companion.android",
                "no.easee.apps.easee.users"
            )
            putPackages(
                AppCategory.HOMELAB,
                "com.frigateviewer",
                "com.github.gotify",
                "com.jgeek00.adguard_home_manager",
                "com.opnsensemanager.app",
                "com.proxmox.app.pve_flutter_frontend",
                "com.ubnt.easyunifi"
            )
            putPackages(
                AppCategory.FINANCE,
                "atws.app",
                "com.banknorwegian",
                "com.danskebank.mobilebank3.no",
                "com.google.android.apps.walletnfcrel",
                "com.handelsbanken.mobile.android.nopriv",
                "com.nordnet",
                "com.paypal.android.p2pmobile",
                "com.transferwise.android",
                "no.apps.dnbnor",
                "no.dnb.vipps",
                "no.nordea.mobilebank",
                "no.sparebank1.mobilbank",
                "no.vipps.bankid"
            )
            putPackages(
                AppCategory.SHOPPING,
                "com.alibaba.aliexpresshd",
                "com.amazon.mShop.android.shopping",
                "com.app.tgtg",
                "com.ebay.mobile",
                "com.einnovation.temu",
                "com.ingka.ikea.app",
                "com.samsung.ecomm.global.gbr",
                "com.shopify.arrive",
                "com.tise.tise",
                "no.coop.members",
                "no.finn.android",
                "no.norgesgruppen.apps.kiwipluss.production",
                "no.norgesgruppen.apps.spar",
                "no.norgesgruppen.apps.trumf.trumf",
                "no.rema.bella",
                "se.prisjakt.pricespy"
            )
            putPackages(
                AppCategory.TRAVEL_NAVIGATION,
                "cgeo.geocaching",
                "com.airbnb.android",
                "com.europark.mobilparkering",
                "com.google.android.apps.maps",
                "com.google.android.apps.mapslite",
                "com.groundspeak.geocaching.intro",
                "com.lufthansa.android.lufthansa",
                "com.norwegian.travelassistant",
                "com.ryde_android",
                "com.tripadvisor.tripadvisor",
                "com.tripit",
                "ctrip.english",
                "dk.bnr.taxifix",
                "io.voiapp.voi",
                "net.easypark.android",
                "no.kolumbus.kolumbusbillett",
                "se.sas.android"
            )
            putPackages(
                AppCategory.MUSIC_AUDIO,
                "com.sec.android.app.voicenote",
                "com.zarz.spotiflac"
            )
            putPackages(
                AppCategory.VIDEO,
                "app.morphe.android.youtube",
                "app.revanced.android.youtube",
                "com.apple.atve.androidtv.appletv",
                "com.michaldrabik.showly2",
                "com.michaldrabik.showly_oss",
                "com.mxtech.videoplayer.pro",
                "com.netflix.mediaclient",
                "com.plexapp.android",
                "com.skyshowtime.skyshowtime.google",
                "com.stremio.one",
                "com.viaplay.android",
                "com.wbd.stream",
                "no.nrk.tv",
                "no.tv2.sumo",
                "org.schabi.newpipe",
                "tv.trakt.trakt"
            )
            putPackages(
                AppCategory.PHOTOS,
                "com.samsung.android.imageenhancer",
                "com.sec.android.app.camera",
                "com.sec.android.gallery3d"
            )
            putPackages(
                AppCategory.READING,
                "app.mihon",
                "com.audible.application",
                "com.bookbites.library",
                "com.contentsfirst.tappytoon",
                "com.gtl.nextory",
                "com.kobobooks.android",
                "com.overdrive.mobile.android.libby",
                "grit.storytel.app",
                "io.elevenlabs.readerapp",
                "jp.co.shueisha.mangaplus",
                "no.ebok.android"
            )
            putPackages(
                AppCategory.DEVELOPMENT,
                "com.github.android",
                "com.termux",
                "com.termux.x11"
            )
            putPackages(
                AppCategory.TOOLS,
                "com.android.settings",
                "com.looker.droidify",
                "com.samsung.android.goodlock",
                "com.samsung.android.pentastic",
                "com.samsung.android.sidegesturepad",
                "com.samsung.android.soundassistant",
                "com.samsung.knox.securefolder",
                "com.samsung.systemui.notilus",
                "com.zacharee1.systemuituner",
                "dev.imranr.obtainium.fdroid",
                "dev.zwander.installwithoptions",
                "ginlemon.iconpackstudio",
                "io.github.muntashirakon.AppManager",
                "moe.shizuku.privileged.api",
                "org.adaway",
                "org.fdroid.fdroid",
                "yuh.yuh.finelock"
            )
        }

    private fun MutableMap<String, AppCategory>.putPackages(
        category: AppCategory,
        vararg packageNames: String
    ) {
        packageNames.forEach { packageName ->
            put(packageName, category)
        }
    }

    private fun installedApp(
        packageName: String,
        label: String = "Example",
        platformCategory: PlatformAppCategory = PlatformAppCategory.UNDEFINED
    ): InstalledApp =
        InstalledApp(
            id = AppId(packageName),
            launchTargetId = LaunchTargetId(packageName, "$packageName.MainActivity"),
            label = label,
            platformCategory = platformCategory
        )
}
