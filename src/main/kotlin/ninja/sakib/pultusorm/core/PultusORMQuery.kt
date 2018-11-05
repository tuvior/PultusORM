package ninja.sakib.pultusorm.core

import ninja.sakib.pultusorm.callbacks.Callback
import ninja.sakib.pultusorm.exceptions.PultusORMException
import ninja.sakib.pultusorm.system.SqliteSystem
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import kotlin.concurrent.thread
import kotlin.properties.Delegates

/**
 * := Coded with love by Sakib Sami on 9/27/16.
 * := s4kibs4mi@gmail.com
 * := www.sakib.ninja
 * := Coffee : Dream : Code
 */

/**
 *  Base Class of PultusORM API
 *  to execute queries
 */
class PultusORMQuery(connection: Connection) {
    /**
     * Query Type
     */
    enum class Type {
        SAVE,
        UPDATE,
        DELETE,
        DROP
    }

    /**
     * Sort Type
     */
    enum class Sort {
        ASCENDING,
        DESCENDING
    }

    private var statement: Statement by Delegates.notNull<Statement>()

    init {
        this.statement = connection.createStatement()
    }

    /**
     * Method to save value
     * @param clazz
     */
    fun save(clazz: Any): Boolean {
        try {
            createTable(clazz)

            statement.execute(Builder().insert(clazz))
            log(this.javaClass.simpleName, "Inserted into ${clazz.javaClass.simpleName} - Succeed")
            return true
        } catch (exception: Exception) {
            log(this.javaClass.simpleName, "Inserted into ${clazz.javaClass.simpleName} - Failed <${exception.message}>")
        }
        return false
    }

    /**
     * Method to save value with callback
     * @param clazz
     * @param callback
     */
    fun save(clazz: Any, callback: Callback) {
        thread(start = true) {
            try {
                createTable(clazz)

                statement.execute(Builder().insert(clazz))
                callback.onSuccess(Type.SAVE)
            } catch (exception: Exception) {
                val ormException: PultusORMException = PultusORMException(exception.message!!)
                callback.onFailure(Type.SAVE, ormException)
            }
        }
    }

    /**
     * Method to get value based on condition
     * @param clazz
     * @param condition
     * @return MutableList of type Any
     */
    fun get(clazz: Any, condition: PultusORMCondition): MutableList<Any> {
        createTable(clazz)

        val query = Builder().select(clazz, condition)
        try {
            val result: ResultSet = statement.executeQuery(query)
            return resultSetToList(result, clazz)
        } catch (exception: Exception) {
            throwback("Malformed query <$query>. Caused by ${exception.message}")
        }
        return mutableListOf()
    }

    /**
     * Method to get value
     * @param clazz
     * @return MutableList of type Any
     */
    fun get(clazz: Any): MutableList<Any> {
        createTable(clazz)

        val query = Builder().select(clazz)
        try {
            val result: ResultSet = statement.executeQuery(query)
            return resultSetToList(result, clazz)
        } catch (exception: Exception) {
            exception.printStackTrace()
            throwback("Malformed query <$query>. Caused by ${exception.message}")
        }
        return mutableListOf()
    }

    /**
     * Method to update value
     * @param clazz
     * @param updateQuery update parameters
     */
    fun update(clazz: Any, updateQuery: PultusORMUpdater): Boolean {
        createTable(clazz)

        try {
            statement.execute(Builder().update(clazz, updateQuery))
            return true
        } catch (exception: Exception) {

            throwback("Malformed update query. (${exception.message})\n${Builder().update(clazz, updateQuery)}")
        }
        return false
    }

    /**
     * Method to update value with callback
     * @param clazz
     * @param updateQuery update parameters
     * @param callback
     */
    fun update(clazz: Any, updateQuery: PultusORMUpdater, callback: Callback) {
        thread(start = true) {
            createTable(clazz)

            try {
                statement.execute(Builder().update(clazz, updateQuery))
                callback.onSuccess(Type.UPDATE)
            } catch (exception: Exception) {
                val ormException: PultusORMException = PultusORMException(exception.message!!)
                callback.onFailure(Type.UPDATE, ormException)
            }
        }
    }

    /**
     * Method to delete value
     * @param clazz
     */
    fun delete(clazz: Any): Boolean {
        createTable(clazz)

        try {
            statement.execute(Builder().delete(clazz))
            log(this.javaClass.simpleName, "Table ${parseClassName(clazz)} deleted - Succeed")
            return true
        } catch (exception: Exception) {
            log(this.javaClass.simpleName, "Table ${parseClassName(clazz)} data deleted - Failed <${exception.message}>")
        }
        return false
    }

    /**
     * Method to delete value based on condition
     * @param clazz
     * @param condition condition to update value
     */
    fun delete(clazz: Any, condition: PultusORMCondition): Boolean {
        createTable(clazz)

        try {
            statement.execute(Builder().delete(clazz, condition))
            log(this.javaClass.simpleName, "Table ${parseClassName(clazz)} delete - Succeed")
            return true
        } catch (exception: Exception) {
            log(this.javaClass.simpleName, "Table ${parseClassName(clazz)} delete - Failed <${exception.message}>")
        }
        return false
    }

    /**
     * Method to delete value with callback
     * @param clazz
     * @param callback
     */
    fun delete(clazz: Any, callback: Callback) {
        thread(start = true) {
            createTable(clazz)

            try {
                statement.execute(Builder().delete(clazz))
                callback.onSuccess(Type.DELETE)
            } catch (exception: Exception) {
                val ormException: PultusORMException = PultusORMException(exception.message!!)
                callback.onFailure(Type.DELETE, ormException)
            }
        }
    }

    /**
     * Method to delete value based on condition with callback
     * @param clazz
     * @param condition
     * @param callback
     */
    fun delete(clazz: Any, condition: PultusORMCondition, callback: Callback) {
        thread(start = true) {
            createTable(clazz)

            try {
                statement.execute(Builder().delete(clazz, condition))
                callback.onSuccess(Type.DELETE)
            } catch (exception: Exception) {
                val ormException: PultusORMException = PultusORMException(exception.message!!)
                callback.onFailure(Type.DELETE, ormException)
            }
        }
    }

    /**
     * Method to delete table
     * @param clazz
     */
    fun drop(clazz: Any): Boolean {
        try {
            if (isTableExists(clazz.javaClass.simpleName))
                statement.execute(Builder().drop(clazz))
            return true
        } catch (exception: Exception) {
            throwback("Malformed drop query.")
        }
        return false
    }

    /**
     * Method to delete table with callback
     * @param clazz
     * @param callback
     */
    fun drop(clazz: Any, callback: Callback) {
        thread(start = true) {
            try {
                if (isTableExists(clazz.javaClass.simpleName)) {
                    statement.execute(Builder().drop(clazz))
                    callback.onSuccess(Type.DROP)
                } else {
                    val ormException: PultusORMException = PultusORMException("Table ${clazz.javaClass.simpleName} not found.")
                    callback.onFailure(Type.DROP, ormException)
                }
            } catch (exception: Exception) {
                val ormException: PultusORMException = PultusORMException(exception.message!!)
                callback.onFailure(Type.DROP, ormException)
            }
        }
    }

    /**
     * Method to get count of values
     * @param clazz
     * @return Long
     */
    fun count(clazz: Any): Long {
        createTable(clazz)

        var counter: Long = 0
        val query = Builder().select(clazz)
        try {
            val result: ResultSet = statement.executeQuery(query)
            while (result.next()) {
                counter++
            }
        } catch (exception: Exception) {
            throwback("Malformed query <$query>.")
        }
        return counter
    }

    /**
     * Method to check is table exists or not
     * @param tableName name of the table
     * @return Boolean
     */
    private fun isTableExists(tableName: String): Boolean {
        val result = statement.executeQuery("SELECT * FROM ${SqliteSystem.getTableName()}" +
                " WHERE type='${SqliteSystem.Type.TABLE.name.toLowerCase()}' AND name='$tableName';")
        val isExist = result.next()

        if (isExist)
            log(this.javaClass.simpleName, "table $tableName already exists.")
        else log(this.javaClass.simpleName, "table $tableName not exists")

        result.close()
        return isExist
    }

    /**
     * Method to create table
     * @param clazz
     */
    private fun createTable(clazz: Any) {
        statement.execute(Builder().create(clazz))
        log(this.javaClass.simpleName, "table ${clazz.javaClass.simpleName} has been created.")
    }

    inner class Builder {
        fun create(clazz: Any): String {
            if (clazz.javaClass.declaredFields.isEmpty())
                throwback("Class without field is not allowed.")

            val queryBuilder = StringBuilder()
            queryBuilder.append("CREATE TABLE IF NOT EXISTS ${clazz.javaClass.simpleName}(")

            val fields = clazz.javaClass.declaredFields + clazz.javaClass.superclass.declaredFields

            var isFirst = true
            for (field in fields) {
                if (isIgnoreField(field).not() && isAutoGeneratedClassMetaDataField(field).not()) {
                    if (isFirst.not())
                        queryBuilder.append(", ")

                    val fieldPart: String = "${field.name} ${typeToSQL(field.genericType)} ${toPrimaryKey(field)} " +
                            "${toAutoIncrement(field)} ${toNotNull(field)} ${toUnique(field)}"
                    queryBuilder.append(fieldPart.trim())

                    isFirst = false
                }
            }
            queryBuilder.append(");")
            return queryBuilder.toString()
        }

        fun insert(clazz: Any): String {
            val objectAsJson = objectAsJson(clazz)
            val queryBuilder = StringBuilder()
            queryBuilder.append("INSERT INTO ${clazz.javaClass.simpleName}")

            val keyBuilder = StringBuilder("(")
            val valueBuilder = StringBuilder(" VALUES(")

            val fields = clazz.javaClass.declaredFields + clazz.javaClass.superclass.declaredFields

            var isFirst = true
            for (field in fields) {
                if (objectAsJson.get(field.name) != null && isIgnoreField(field).not() && isAutoGeneratedClassMetaDataField(field).not()) {
                    if (isAutoIncrement(field))
                        continue

                    if (isFirst.not()) {
                        keyBuilder.append(",")
                        valueBuilder.append(",")
                    }
                    log("fieldname", field.name)

                    keyBuilder.append(field.name)
                    if (objectAsJson.get(field.name).isString) {
                        valueBuilder.append("'${objectAsJson.getString(field.name, "")}'")
                    } else if (objectAsJson.get(field.name).isBoolean) {
                        if (objectAsJson.getBoolean(field.name, false))
                            valueBuilder.append("1")
                        else valueBuilder.append("0")
                    } else valueBuilder.append(objectAsJson.get(field.name))

                    isFirst = false
                }
            }

            keyBuilder.append(")")
            valueBuilder.append(")")

            queryBuilder.append(keyBuilder)
            queryBuilder.append(valueBuilder)
            queryBuilder.append(";")

            log(this.javaClass.simpleName, queryBuilder.toString())
            return queryBuilder.toString()
        }

        fun select(clazz: Any, condition: PultusORMCondition): String {
            if (condition.rawQuery().trim().isNotEmpty()) {
                return "SELECT * FROM ${clazz.javaClass.simpleName} ${condition.rawQuery()};"
            } else return select(clazz)
        }

        fun select(clazz: Any): String {
            return "SELECT * FROM ${clazz.javaClass.simpleName};"
        }

        fun update(clazz: Any, updateQuery: PultusORMUpdater): String {
            return buildString {
                append("UPDATE ${clazz.javaClass.simpleName} SET ")
                append(updateQuery.updateQuery())
                if (updateQuery.condition() != null)
                    append(" ${updateQuery.condition()!!.rawQuery()}")
                append(";")
            }
        }

        fun delete(clazz: Any): String {
            return "DELETE FROM ${clazz.javaClass.simpleName};"
        }

        fun delete(clazz: Any, condition: PultusORMCondition): String {
            return "DELETE FROM ${clazz.javaClass.simpleName} ${condition.rawQuery()};"
        }

        fun drop(clazz: Any): String {
            return "DROP TABLE ${clazz.javaClass.simpleName};"
        }
    }
}
