package com.ammar.wallflow.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ammar.wallflow.data.db.manualmigrations.MIGRATION_1_2
import com.ammar.wallflow.data.db.manualmigrations.MIGRATION_3_4
import java.io.IOException
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val testDbName = "migration-test"
    private val allManualMigrations = arrayOf(
        MIGRATION_1_2,
        MIGRATION_3_4,
    )

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = AppDatabase::class.java,
        specs = emptyList(),
        openFactory = FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        helper.createDatabase(testDbName, 1).apply {
            close()
        }
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            testDbName,
        ).addMigrations(*allManualMigrations).build().apply {
            openHelper.writableDatabase.close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate3To4() {
        helper.createDatabase(testDbName, 3).apply {
            execSQL(
                // language=sql
                """
                    INSERT INTO tags
                        ("id", "wallhaven_id", "name", "alias", "category_id", "category", "purity", "created_at")
                    VALUES
                        ('1', '8099', 'Tifa Lockhart', '', '49', 'Fictional Characters', 'sfw', '1411675211000'),
                        ('2', '37', 'nature', '', '5', 'Nature', 'sfw', '1391369096000'),
                        ('3', '65348', '4K', '', '2', 'Art & Design', 'sfw', '1503522417000'),
                        ('4', '323', 'artwork', '', '2', 'Art & Design', 'sfw', '1392110323000'),
                        ('5', '175', 'anime boys', '', '20', 'Characters', 'sfw', '1391907044000'),
                        ('6', '2729', 'sky', '', '5', 'Nature', 'sfw', '1403281238000'),
                        ('7', '328', 'mountains', '', '41', 'Landscapes', 'sfw', '1392135639000'),
                        ('8', '314', 'car', '', '54', 'Cars & Motorcycles', 'sfw', '1392093149000'),
                        ('9', '3834', 'schoolgirl', '', '7', 'People', 'sfw', '1410116773000'),
                        ('10', '141554', 'Kafka (Honkai: Star Rail)', '', '20', 'Characters', 'sfw', '1676084898000');
                """.trimIndent(),
            )
            execSQL(
                // language=sql
                """
                    INSERT INTO popular_tags
                        ("id", "tag_id")
                    VALUES
                        ('201', '1'),
                        ('202', '2'),
                        ('203', '3'),
                        ('204', '4'),
                        ('205', '5'),
                        ('206', '6'),
                        ('207', '7'),
                        ('208', '8'),
                        ('209', '9'),
                        ('210', '10');
                """.trimIndent(),
            )
            execSQL(
                // language=sql
                """
                    INSERT INTO search_query
                        ("id", "query_string", "last_updated_on")
                    VALUES
                        (
                            '1',
                            'includedTags=test&excludedTags=&username=&tagId=&wallpaperId=&categories=anime%2Cgeneral%2Cpeople&purity=sfw&sorting=toplist&order=desc&topRange=1d&atleast=&resolutions=&ratios=&colors=&seed=',
                            '12345'
                        ),
                        (
                            '2',
                            'includedTags=test1&excludedTags=&username=&tagId=&wallpaperId=&categories=anime%2Cgeneral%2Cpeople&purity=sfw&sorting=toplist&order=desc&topRange=1d&atleast=&resolutions=&ratios=&colors=&seed=',
                            '12345'
                        ),
                        (
                            '3',
                            'includedTags=test2&excludedTags=&username=&tagId=&wallpaperId=&categories=anime%2Cgeneral%2Cpeople&purity=sfw&sorting=toplist&order=desc&topRange=1d&atleast=&resolutions=&ratios=&colors=&seed=',
                            '12345'
                        ),
                        (
                            '4',
                            'includedTags=&excludedTags=&username=&tagId=&wallpaperId=&categories=anime%2Cgeneral%2Cpeople&purity=sfw&sorting=toplist&order=desc&topRange=1d&atleast=&resolutions=&ratios=&colors=&seed=',
                            '12345'
                        );
                """.trimIndent(),
            )
            execSQL(
                // language=sql
                """
                    INSERT INTO search_query_remote_keys
                        ("id", "search_query_id", "next_page_number")
                    VALUES
                        ('11', '2', '5'),
                        ('12', '1', NULL),
                        ('13', '3', '5');
                """.trimIndent(),
            )
            execSQL(
                // language=sql
                """
                    INSERT INTO saved_searches
                        ("id", "name", "query", "filters")
                    VALUES
                        (
                            '1',
                            'home',
                            'test',
                            'includedTags=&excludedTags=&username=&tagId=&wallpaperId=&categories=anime%2Cgeneral%2Cpeople&purity=sfw&sorting=toplist&order=desc&topRange=1d&atleast=&resolutions=&ratios=&colors=&seed='
                        );
                """.trimIndent(),
            )
            execSQL(
                // language=sql
                """
                    INSERT INTO search_history
                        ("id", "query", "filters", "last_updated_on")
                    VALUES
                        (
                            '1',
                            'test',
                            'includedTags=&excludedTags=&username=&tagId=&wallpaperId=&categories=anime%2Cgeneral%2Cpeople&purity=sfw&sorting=toplist&order=desc&topRange=1d&atleast=&resolutions=&ratios=&colors=&seed=',
                            '12345'
                        );
                """.trimIndent(),
            )
            close()
        }
        helper.runMigrationsAndValidate(
            testDbName,
            4,
            true,
            MIGRATION_3_4,
        ).use { db ->
            // language=sql
            db.query("SELECT COUNT(*) from wallhaven_popular_tags").use {
                it.moveToFirst()
                val count = it.getInt(0)
                assertEquals(10, count)
            }

            // language=sql
            db.query("SELECT COUNT(*) from wallhaven_tags").use {
                it.moveToFirst()
                val count = it.getInt(0)
                assertEquals(10, count)
            }

            // language=sql
            db.query("SELECT COUNT(*) from search_query_remote_keys").use {
                it.moveToFirst()
                val count = it.getInt(0)
                assertEquals(3, count)
            }

            // language=sql
            db.query("SELECT * from search_query_remote_keys").use {
                it.moveToFirst()
                val id = it.getInt(0)
                assertEquals(11, id)
                val searchQueryId = it.getInt(1)
                assertEquals(2, searchQueryId)
                val nextPageNumber = it.getInt(2)
                assertEquals(5, nextPageNumber)
            }

            db.query(
                // language=sql
                "SELECT * from search_query",
            ).use {
                it.moveToFirst()
                val queryString = it.getString(
                    it.getColumnIndexOrThrow("query_string"),
                )
                assertEquals(
                    "{\"filters\":{\"includedTags\":[\"test\"]," +
                        "\"sorting\":\"TOPLIST\",\"topRange\":\"ONE_DAY\"}}",
                    queryString,
                )
            }

            db.query(
                // language=sql
                "SELECT * from saved_searches",
            ).use {
                it.moveToFirst()
                val filtersStr = it.getString(
                    it.getColumnIndexOrThrow("filters"),
                )
                assertEquals(
                    "{\"sorting\":\"TOPLIST\",\"topRange\":\"ONE_DAY\"}",
                    filtersStr,
                )
            }

            db.query(
                // language=sql
                "SELECT * from search_history",
            ).use {
                it.moveToFirst()
                val filtersStr = it.getString(
                    it.getColumnIndexOrThrow("filters"),
                )
                assertEquals(
                    "{\"sorting\":\"TOPLIST\",\"topRange\":\"ONE_DAY\"}",
                    filtersStr,
                )
            }
        }
    }
}
