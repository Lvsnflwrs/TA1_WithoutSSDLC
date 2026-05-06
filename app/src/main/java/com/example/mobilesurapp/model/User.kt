package com.example.mobilesurapp.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(
    tableName = "users",
    indices = [androidx.room.Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,

    @SerializedName("user_id")
    val userId: Int? = null,

    @SerializedName("admin_id")
    @ColumnInfo(name = "admin_id")
    val adminId: Int? = null,

    val name: String,
    val email: String,
    val phone: String,
    val embeddings: FloatArray,
    val role: String = "admin"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (localId != other.localId) return false
        if (userId != other.userId) return false
        if (adminId != other.adminId) return false
        if (name != other.name) return false
        if (email != other.email) return false
        if (phone != other.phone) return false
        if (!embeddings.contentEquals(other.embeddings)) return false
        if (role != other.role) return false

        return true
    }

    override fun hashCode(): Int {
        var result = localId
        result = 31 * result + (userId ?: 0)
        result = 31 * result + name.hashCode()
        result = 31 * result + email.hashCode()
        result = 31 * result + phone.hashCode()
        result = 31 * result + embeddings.contentHashCode()
        result = 31 * result + role.hashCode()
        return result
    }
}
