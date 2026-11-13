package br.com.zamfir.comprarei.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.zamfir.comprarei.core.database.entity.UserInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface UserInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(userInfo: UserInfo)

    @Query("UPDATE USER_INFO SET profilePicture = :path")
    fun updateProfilePicturePath(path : String)

    @Query("SELECT * FROM USER_INFO")
    fun getUserInfo() : UserInfo?

    @Query("SELECT * FROM USER_INFO")
    fun observeUserInfo(): Flow<UserInfo?>

}