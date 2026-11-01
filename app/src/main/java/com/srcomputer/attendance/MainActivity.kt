package com.srcomputer.attendance

import android.app.*
import android.os.Bundle
import android.content.*
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val NAVY = Color.rgb(23, 61, 117)
private val PALE = Color.rgb(232, 240, 250)
private val CODES = listOf("P", "A", "Va", "Ad", "Mo", "ENO", "Off")
private fun codeColor(code: String): Int = when(code) {
    "P" -> Color.rgb(18,182,232); "A" -> Color.YELLOW; "Va" -> Color.rgb(229,183,183)
    "Ad" -> Color.rgb(255,75,75); "Mo" -> Color.rgb(54,180,91); "ENO" -> Color.rgb(0,168,216)
    "Off" -> Color.rgb(217,170,170); else -> Color.WHITE
}

data class Student(val id: Long, val name: String, val photo: String)

class AttendanceDb(ctx: Context) : SQLiteOpenHelper(ctx, "sr_attendance.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE students(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, photo TEXT DEFAULT '', active INTEGER DEFAULT 1)")
        db.execSQL("CREATE TABLE attendance(student_id INTEGER NOT NULL, day TEXT NOT NULL, code TEXT NOT NULL, PRIMARY KEY(student_id,day))")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    fun students(): List<Student> {
        val out = mutableListOf<Student>(); readableDatabase.rawQuery("SELECT id,name,photo FROM students WHERE active=1 ORDER BY id", null).use { c ->
            while(c.moveToNext()) out.add(Student(c.getLong(0), c.getString(1), c.getString(2) ?: ""))
        }; return out
    }
    fun add(name: String, photo: String) { writableDatabase.execSQL("INSERT INTO students(name,photo) VALUES(?,?)", arrayOf(name, photo)) }
    fun edit(id: Long, name: String, photo: String) { writableDatabase.execSQL("UPDATE students SET name=?,photo=? WHERE id=?", arrayOf(name, photo, id)) }
    fun delete(id: Long) { writableDatabase.execSQL("UPDATE students SET active=0 WHERE id=?", arrayOf(id)) }
    fun get(id: Long, day: String): String = readableDatabase.rawQuery("SELECT code FROM attendance WHERE student_id=? AND day=?", arrayOf(id.toString(),day)).use { if(it.moveToFirst()) it.getString(0) else "" }
    fun set(id: Long, day: String, code: String) {
        if(code.isBlank()) writableDatabase.execSQL("DELETE FROM attendance WHERE student_id=? AND day=?", arrayOf(id,day))
        else writableDatabase.execSQL("INSERT OR REPLACE INTO attendance(student_id,day,code) VALUES(?,?,?)", arrayOf(id,day,code))
    }
    fun counts(id: Long, month: String? = null): Map<String,Int> {
        val m = mutableMapOf<String,Int>(); CODES.forEach { m[it]=0 }
        val sql = if(month == null) "SELECT code,COUNT(*) FROM attendance WHERE student_id=? GROUP BY code" else "SELECT code,COUNT(*) FROM attendance WHERE student_id=? AND substr(day,1,7)=? GROUP BY code"
        val args = if(month == null) arrayOf(id.toString()) else arrayOf(id.toString(),month)
        readableDatabase.rawQuery(sql,args).use { c -> while(c.moveToNext()) m[c.getString(0)] = c.getInt(1) }
        return m
    }
}

class MainActivity : Activity() {
    private lateinit var db: AttendanceDb
    private lateinit var root: LinearLayout
    private lateinit var monthText: TextView
    private lateinit var list: LinearLayout
    private lateinit var summary: TextView
    private var selectedMonth = YearMonth.now()
    private var editingPhoto = ""
    private val pickPhoto = 77
    private val blueText get() = NAVY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AttendanceDb(this)
        render()
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun text(s: String, size: Float=14f, color: Int=Color.BLACK, bold: Boolean=false): TextView = TextView(this).apply {
        text=s; textSize=size; setTextColor(color); if(bold) setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER_VERTICAL
    }
    private fun button(s: String, action: () -> Unit): Button = Button(this).apply {
        text=s; isAllCaps=false; textSize=13f; setOnClickListener { action() }
    }
    private fun render() {
        root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(245,248,253)) }
        val header = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(16),dp(12),dp(16),dp(8)); setBackgroundColor(Color.rgb(242,246,252)) }
        header.addView(text("SR COMPUTER",22f,NAVY,true)); header.addView(text("Attendance Sheet",20f,NAVY,true))
        val monthRow = LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL; orientation=LinearLayout.HORIZONTAL }
        monthRow.addView(button("‹") { selectedMonth=selectedMonth.minusMonths(1); render() })
        monthText=text("${selectedMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${selectedMonth.year}",16f,NAVY,true)
        monthRow.addView(monthText, LinearLayout.LayoutParams(0,dp(48),1f).apply { gravity=Gravity.CENTER })
        monthRow.addView(button("›") { selectedMonth=selectedMonth.plusMonths(1); render() })
        monthRow.addView(button("Today") { selectedMonth=YearMonth.now(); render() })
        header.addView(monthRow); root.addView(header)
        val legend=text("P Present   •   A Absent   •   Va Vacation   •   Ad Admission   •   Mo Month   •   ENO Electricity No.   •   Off Holiday",11f,Color.RED,true)
        legend.setPadding(dp(10),dp(7),dp(10),dp(7)); root.addView(legend)
        val actions = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER; setPadding(dp(6),dp(3),dp(6),dp(3)) }
        actions.addView(button("+ Add Student") { studentDialog(null) })
        actions.addView(button("PDF") { exportPdf() })
        actions.addView(button("Backup") { backupInfo() })
        root.addView(actions)
        val scroll=ScrollView(this); list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(8),dp(4),dp(8),dp(8)) }
        scroll.addView(list); root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        summary=text("Overall totals will appear here",13f,NAVY,true); summary.setPadding(dp(12),dp(12),dp(12),dp(12)); summary.setBackgroundColor(PALE); root.addView(summary)
        setContentView(root)
        fillStudents()
    }
    private fun fillStudents() {
        val students=db.students(); val days=selectedMonth.lengthOfMonth()
        students.forEachIndexed { index, st ->
            val card=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(10),dp(8),dp(10),dp(8)); setBackgroundColor(Color.WHITE) }
            val top=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
            val avatar=ImageView(this).apply { scaleType=ImageView.ScaleType.CENTER_CROP; setBackgroundColor(PALE) }
            if(st.photo.isNotBlank()) runCatching { avatar.setImageURI(Uri.parse(st.photo)) }
            top.addView(avatar,LinearLayout.LayoutParams(dp(52),dp(52)))
            val name=text("${index+1}. ${st.name}",16f,NAVY,true); name.setPadding(dp(10),0,0,0); top.addView(name,LinearLayout.LayoutParams(0,dp(52),1f))
            top.addView(button("Edit") { studentDialog(st) }); top.addView(button("⋮") { studentMenu(st) })
            card.addView(top)
            val daysScroll=HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled=true }
            val dayRow=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
            for(day in 1..days) {
                val date=selectedMonth.atDay(day); var code=db.get(st.id,date.toString())
                if(date.dayOfWeek.value==5 && code.isBlank()) code="Off"
                val cell=TextView(this).apply {
                    text="${date.dayOfWeek.getDisplayName(TextStyle.SHORT,Locale.ENGLISH)}\n$day\n${if(code.isBlank()) "—" else code}"
                    textSize=11f; gravity=Gravity.CENTER; setTextColor(Color.BLACK); setBackgroundColor(codeColor(code)); setPadding(dp(5),dp(8),dp(5),dp(8))
                    setOnClickListener { attendanceDialog(st,date.toString()) }
                }
                val lp=LinearLayout.LayoutParams(dp(57),dp(64)); lp.setMargins(dp(2),dp(2),dp(2),dp(2)); dayRow.addView(cell,lp)
            }
            daysScroll.addView(dayRow); card.addView(daysScroll)
            val counts=db.counts(st.id,selectedMonth.toString())
            card.addView(text("This month:  P ${counts["P"]}   A ${counts["A"]}   Va ${counts["Va"]}   Ad ${counts["Ad"]}   Mo ${counts["Mo"]}   ENO ${counts["ENO"]}   Off ${counts["Off"]}",12f,NAVY,true))
            val lp=LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,dp(4),0,dp(6)); list.addView(card,lp)
        }
        if(students.isEmpty()) list.addView(text("No students yet. Tap + Add Student to begin.",15f,NAVY))
        val all=CODES.associateWith { code -> students.sumOf { db.counts(it.id)[code] ?: 0 } }
        summary.text="OVERALL (All months)\n"+CODES.joinToString("     ") { "$it ${all[it]}" }
    }
    private fun attendanceDialog(st: Student, day: String) {
        val choices=arrayOf("Clear", "P – Present", "A – Absent", "Va – Vacation", "Ad – Admission", "Mo – Month", "ENO – Electricity No.", "Off – Holiday")
        AlertDialog.Builder(this).setTitle("${st.name}\n$day").setItems(choices) { _, which -> db.set(st.id,day,if(which==0) "" else CODES[which-1]); render() }.show()
    }
    private fun studentMenu(st: Student) {
        AlertDialog.Builder(this).setItems(arrayOf("Edit profile / photo","Delete student")) { _, which -> if(which==0) studentDialog(st) else AlertDialog.Builder(this).setMessage("Delete ${st.name} from active student list?").setNegativeButton("Cancel",null).setPositiveButton("Delete") { _,_-> db.delete(st.id); render() }.show() }.show()
    }
    private fun studentDialog(st: Student?) {
        editingPhoto=st?.photo ?: ""
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(20),dp(8),dp(20),0) }
        val name=EditText(this).apply { hint="Student name"; setText(st?.name ?: "") }; box.addView(name)
        val photoLabel=text(if(editingPhoto.isBlank()) "No photo selected" else "Photo selected",13f,NAVY); box.addView(photoLabel)
        box.addView(button("Choose student photo") { val i=Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type="image/*"; addCategory(Intent.CATEGORY_OPENABLE) }; startActivityForResult(i,pickPhoto) })
        AlertDialog.Builder(this).setTitle(if(st==null) "Add Student" else "Edit Student").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save") { _,_->
            val n=name.text.toString().trim(); if(n.isNotEmpty()) { if(st==null) db.add(n,editingPhoto) else db.edit(st.id,n,editingPhoto); render() } else Toast.makeText(this,"Enter student name",Toast.LENGTH_SHORT).show()
        }.show()
    }
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) { super.onActivityResult(requestCode,resultCode,data); if(requestCode==pickPhoto && resultCode==RESULT_OK) { data?.data?.let { uri -> runCatching { contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION) }; editingPhoto=uri.toString(); Toast.makeText(this,"Photo selected. Save the student profile.",Toast.LENGTH_SHORT).show() } } }
    private fun exportPdf() {
        val doc=PdfDocument(); val pageInfo=PdfDocument.PageInfo.Builder(595,842,1).create(); val page=doc.startPage(pageInfo); val p=page.canvas; val paint=android.graphics.Paint().apply { color=NAVY; textSize=16f; typeface=Typeface.DEFAULT_BOLD }
        p.drawText("SR COMPUTER - ATTENDANCE REPORT",30f,40f,paint); paint.textSize=11f; paint.typeface=Typeface.DEFAULT
        p.drawText("${selectedMonth.month.getDisplayName(TextStyle.FULL,Locale.ENGLISH)} ${selectedMonth.year}",30f,62f,paint)
        var y=90f
        db.students().forEach { st ->
            if(y>800) return@forEach
            p.drawText(st.name,30f,y,paint); y+=16
            val c=db.counts(st.id,selectedMonth.toString()); p.drawText("P ${c["P"]}  A ${c["A"]}  Va ${c["Va"]}  Ad ${c["Ad"]}  Mo ${c["Mo"]}  ENO ${c["ENO"]}  Off ${c["Off"]}",45f,y,paint); y+=24
        }
        doc.finishPage(page)
        val file=File(getExternalFilesDir(null),"SR_Attendance_${selectedMonth}.pdf")
        runCatching { FileOutputStream(file).use { doc.writeTo(it) }; Toast.makeText(this,"PDF saved: ${file.absolutePath}",Toast.LENGTH_LONG).show() }.onFailure { Toast.makeText(this,"PDF export failed: ${it.message}",Toast.LENGTH_LONG).show() }
        doc.close()
    }
    private fun backupInfo() { Toast.makeText(this,"Database file: ${getDatabasePath("sr_attendance.db").absolutePath}",Toast.LENGTH_LONG).show() }
}
