package com.alsane.pro

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String = "",
    val purchasePrice: Long,
    val salePrice: Long,
    val quantity: Int,
    val lowStockLimit: Int = 5,
    val unit: String = "قطعة"
)

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Product>>
    @Query("SELECT COUNT(*) FROM products")
    fun observeCount(): Flow<Int>
    @Query("SELECT COALESCE(SUM(quantity * purchasePrice), 0) FROM products")
    fun observeStockCostValue(): Flow<Long>
    @Query("SELECT COUNT(*) FROM products WHERE quantity <= lowStockLimit")
    fun observeLowStockCount(): Flow<Int>
    @Query("SELECT * FROM products WHERE code = :code AND code != '' LIMIT 1")
    suspend fun findByCode(code: String): Product?
    @Insert suspend fun insert(product: Product): Long
    @Update suspend fun update(product: Product): Int
    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}

@Database(entities = [Product::class], version = 1, exportSchema = true)
abstract class BusinessDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    companion object {
        @Volatile private var instance: BusinessDatabase? = null
        fun get(context: Context): BusinessDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, BusinessDatabase::class.java, "mohasabi_business.db")
                .build().also { instance = it }
        }
    }
}
