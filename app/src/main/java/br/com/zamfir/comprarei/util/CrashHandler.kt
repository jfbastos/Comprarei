package br.com.zamfir.comprarei.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import br.com.zamfir.comprarei.view.activity.CrashActivity
import br.com.zamfir.comprarei.view.activity.LoginActivity
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Calendar
import kotlin.system.exitProcess

class CrashHandler(context : Context) : Thread.UncaughtExceptionHandler{

    private val newLine = "\n"
    private val errorMessage = StringBuilder()
    private val softwareInfo = StringBuilder()
    private val dateInfo = StringBuilder()
    var myContext: Context = context

    override fun uncaughtException(thread: Thread, exception: Throwable) {

        val stackTrace = StringWriter()
        exception.printStackTrace(PrintWriter(stackTrace))

        errorMessage.append(stackTrace.toString())

        softwareInfo.append("SDK: ")
        softwareInfo.append(Build.VERSION.SDK_INT)
        softwareInfo.append(newLine)
        softwareInfo.append("Release: ")
        softwareInfo.append(Build.VERSION.RELEASE)
        softwareInfo.append(newLine)
        softwareInfo.append("Incremental: ")
        softwareInfo.append(Build.VERSION.INCREMENTAL)
        softwareInfo.append(newLine)

        dateInfo.append(Calendar.getInstance().time)
        dateInfo.append(newLine)

        Log.d("Error" , errorMessage.toString())
        Log.d("Software" , softwareInfo.toString())
        Log.d("Date" , dateInfo.toString())

        val intent = Intent(myContext , CrashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        intent.putExtra("Error" , errorMessage.toString())
        intent.putExtra("Software" , softwareInfo.toString())
        intent.putExtra("Date" , dateInfo.toString())

        myContext.startActivity(intent)

        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(2)
    }

}