package br.com.zamfir.comprarei.data.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.zamfir.comprarei.data.model.entity.UserInfo

@Dao
interface UserInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(userInfo: UserInfo)

    @Query("UPDATE USER_INFO SET profilePicture = :path")
    fun updateProfilePicturePath(path : String)

    @Query("SELECT * FROM USER_INFO")
    fun getUserInfo() : UserInfo?

}