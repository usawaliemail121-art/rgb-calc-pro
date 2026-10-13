package com.utilitynest.rgbcalcpro

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var tvInput: TextView
    private lateinit var tvPreview: TextView
    private lateinit var tvExpressionTop: TextView
    private lateinit var headerGlow: View
    private lateinit var titleText: TextView

    private lateinit var historyManager: HistoryManager
    private lateinit var settingsManager: SettingsManager
    private lateinit var soundManager: SoundManager
    private val evaluator = ExpressionEvaluator()

    private val input = StringBuilder()
    private var glowAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvInput = findViewById(R.id.tvInput)
        tvPreview = findViewById(R.id.tvPreview)
        tvExpressionTop = findViewById(R.id.tvExpressionTop)
        headerGlow = findViewById(R.id.headerGlow)
        titleText = findViewById(R.id.titleText)

        historyManager = HistoryManager(this)
        settingsManager = SettingsManager(this)
        soundManager = SoundManager(settingsManager)

        input.append(historyManager.loadActiveInput())
        refreshDisplay()
        startRgbGlow()
        wireKeys()

        findViewById<View>(R.id.btnHistory).setOnClickListener { openHistory() }
        findViewById<View>(R.id.btnSettings).setOnClickListener { openSettings() }
    }

    // ---- Keypad wiring -------------------------------------------------

    private fun wireKeys() {
        val digitKeys = mapOf(
            R.id.key0 to "0", R.id.key1 to "1", R.id.key2 to "2", R.id.key3 to "3",
            R.id.key4 to "4", R.id.key5 to "5", R.id.key6 to "6", R.id.key7 to "7",
            R.id.key8 to "8", R.id.key9 to "9"
        )
        for ((id, value) in digitKeys) {
            bindKey(id) { appendToken(value) }
        }

        bindKey(R.id.keyDot) { appendToken(".") }
        bindKey(R.id.keyAdd) { appendOperator("+") }
        bindKey(R.id.keySub) { appendOperator("-") }
        bindKey(R.id.keyMul) { appendOperator("\u00d7") }
        bindKey(R.id.keyDiv) { appendOperator("\u00f7") }
        bindKey(R.id.keyPercent) { appendToken("%") }

        bindKey(R.id.keyParen) { appendParen() }
        bindKey(R.id.keySign) { toggleSign() }
        bindKey(R.id.keyDel) { deleteLast() }
        bindKey(R.id.keyAc) { clearAll() }
        bindKey(R.id.keyEquals) { commitEquals() }
    }

    private fun bindKey(id: Int, action: () -> Unit) {
        val view = findViewById<View>(id)
        applyKeyFeedback(view)
        view.setOnClickListener {
            soundManager.play(it)
            action()
        }
    }

    /** Wraps the neon XML background in a rounded ripple for press feedback. */
    private fun applyKeyFeedback(view: View) {
        val original = view.background ?: return
        val mask = GradientDrawable().apply {
            cornerRadius = 18f * resources.displayMetrics.density
            setColor(Color.WHITE)
        }
        val base = ContextCompat.getColor(this, R.color.neon_cyan)
        val rippleColor = ColorStateList.valueOf(
            Color.argb(90, Color.red(base), Color.green(base), Color.blue(base))
        )
        view.background = RippleDrawable(rippleColor, original, mask)
    }

    // ---- Input handling ------------------------------------------------

    private fun appendToken(token: String) {
        if (token == ".") {
            var i = input.length
            while (i > 0 && (input[i - 1].isDigit() || input[i - 1] == '.')) i--
            val lastNumber = input.substring(i)
            if (lastNumber.contains('.')) return
            if (lastNumber.isEmpty()) {
                input.append("0.")
                refreshDisplay()
                return
            }
        }
        input.append(token)
        refreshDisplay()
    }

    private fun isOperatorChar(c: Char): Boolean =
        c == '+' || c == '-' || c == '\u00d7' || c == '\u00f7'

    /**
     * Appends an operator without letting the user build nonsense such as
     * "5++2" or "5\u00d7\u00f72". A new operator replaces a trailing one, except
     * that a minus is allowed after \u00d7 or \u00f7 so a negative number can be typed.
     */
    private fun appendOperator(op: String) {
        if (input.isEmpty()) {
            if (op == "-") input.append(op)
            refreshDisplay()
            return
        }
        val last = input.last()
        if (last == '(') {
            if (op == "-") input.append(op)
            refreshDisplay()
            return
        }
        if (last == '.') {
            // "5." followed by an operator becomes "5+", never "5.+".
            input.setLength(input.length - 1)
            input.append(op)
            refreshDisplay()
            return
        }
        if (isOperatorChar(last)) {
            if (op == "-" && (last == '\u00d7' || last == '\u00f7')) {
                input.append(op)
            } else {
                input.setCharAt(input.length - 1, op[0])
            }
            refreshDisplay()
            return
        }
        input.append(op)
        refreshDisplay()
    }

    private fun appendParen() {
        val opens = input.count { it == '(' }
        val closes = input.count { it == ')' }
        if (opens > closes && !endsWithOperator()) {
            input.append(")")
        } else {
            input.append("(")
        }
        refreshDisplay()
    }

    private fun endsWithOperator(): Boolean {
        if (input.isEmpty()) return true
        val last = input.last()
        return last == '+' || last == '-' || last == '\u00d7' || last == '\u00f7' || last == '('
    }

    private fun toggleSign() {
        if (input.isEmpty() || endsWithOperator()) {
            input.append("-")
            refreshDisplay()
            return
        }
        var start = input.length
        while (start > 0 && (input[start - 1].isDigit() || input[start - 1] == '.')) start--
        if (start > 0 && input[start - 1] == '-') {
            val before = if (start - 1 > 0) input[start - 2] else null
            if (before == null || before == '+' || before == '-' ||
                before == '\u00d7' || before == '\u00f7' || before == '('
            ) {
                input.deleteCharAt(start - 1)
                refreshDisplay()
                return
            }
        }
        input.insert(start, '-')
        refreshDisplay()
    }

    private fun deleteLast() {
        if (input.isNotEmpty()) {
            input.deleteCharAt(input.length - 1)
            refreshDisplay()
        }
    }

    private fun clearAll() {
        input.setLength(0)
        tvExpressionTop.text = ""
        refreshDisplay()
    }

    private fun commitEquals() {
        val expression = input.toString()
        if (expression.isEmpty()) return
        try {
            val value = evaluator.evaluate(expression)
            val result = ExpressionEvaluator.format(value)
            historyManager.add(HistoryItem(expression, result, System.currentTimeMillis()))
            tvExpressionTop.text = "$expression ="
            input.setLength(0)
            input.append(result)
            tvPreview.text = ""
            tvInput.text = result
            historyManager.saveActiveInput(input.toString())
        } catch (e: ExpressionEvaluator.EvalException) {
            val message = if (e.message == "Cannot divide by 0") {
                getString(R.string.divide_by_zero)
            } else {
                "Invalid expression"
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            tvPreview.text = message
        }
    }

    private fun refreshDisplay() {
        val text = input.toString()
        tvInput.text = if (text.isEmpty()) "0" else text

        if (text.isEmpty()) {
            tvPreview.text = ""
            historyManager.saveActiveInput("")
            return
        }

        historyManager.saveActiveInput(text)

        if (isStructurallyComplete(text)) {
            try {
                val value = evaluator.evaluate(text)
                tvPreview.text = "= " + ExpressionEvaluator.format(value)
            } catch (e: ExpressionEvaluator.EvalException) {
                tvPreview.text = if (e.message == "Cannot divide by 0") {
                    getString(R.string.divide_by_zero)
                } else {
                    ""
                }
            }
        } else {
            tvPreview.text = ""
        }
    }

    /** Only show a live preview when brackets balance and no operator trails. */
    private fun isStructurallyComplete(text: String): Boolean {
        val opens = text.count { it == '(' }
        val closes = text.count { it == ')' }
        if (opens != closes) return false
        val last = text.last()
        if (last == '+' || last == '-' || last == '\u00d7' || last == '\u00f7' ||
            last == '(' || last == '.'
        ) return false
        return true
    }

    // ---- RGB glow ------------------------------------------------------

    private fun startRgbGlow() {
        glowAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 7000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val hue = animation.animatedValue as Float
                titleText.setTextColor(Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                headerGlow.alpha =
                    0.55f + 0.45f * Math.abs(Math.sin(Math.toRadians(hue.toDouble()))).toFloat()
            }
            start()
        }
    }

    // ---- History bottom sheet ------------------------------------------

    private fun openHistory() {
        val dialog = BottomSheetDialog(this)
        val sheet = LayoutInflater.from(this).inflate(R.layout.sheet_history, null)
        val recycler = sheet.findViewById<RecyclerView>(R.id.historyRecycler)!!
        val empty = sheet.findViewById<TextView>(R.id.tvEmptyHistory)!!

        val adapter = HistoryAdapter(mutableListOf()) { item ->
            soundManager.play(sheet)
            input.setLength(0)
            input.append(item.result)
            tvExpressionTop.text = item.expression + " ="
            refreshDisplay()
            dialog.dismiss()
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        fun render() {
            val items = historyManager.getAll()
            adapter.setItems(items)
            empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }

        sheet.findViewById<View>(R.id.btnClearHistory).setOnClickListener {
            historyManager.clear()
            render()
        }

        render()
        dialog.setContentView(sheet)
        dialog.show()
    }

    // ---- Settings bottom sheet -----------------------------------------

    private fun openSettings() {
        val dialog = BottomSheetDialog(this)
        val sheet = LayoutInflater.from(this).inflate(R.layout.sheet_settings, null)

        val soundSwitch = sheet.findViewById<SwitchMaterial>(R.id.switchSound)
        val group = sheet.findViewById<RadioGroup>(R.id.soundGroup)

        soundSwitch.isChecked = settingsManager.soundEnabled
        group.check(
            when (settingsManager.soundType) {
                SettingsManager.TYPE_DIGITAL -> R.id.soundDigital
                SettingsManager.TYPE_BUBBLE -> R.id.soundBubble
                SettingsManager.TYPE_HAPTIC -> R.id.soundHaptic
                else -> R.id.soundMechanical
            }
        )

        soundSwitch.setOnCheckedChangeListener { _, checked ->
            settingsManager.soundEnabled = checked
        }

        group.setOnCheckedChangeListener { _, checkedId ->
            settingsManager.soundType = when (checkedId) {
                R.id.soundDigital -> SettingsManager.TYPE_DIGITAL
                R.id.soundBubble -> SettingsManager.TYPE_BUBBLE
                R.id.soundHaptic -> SettingsManager.TYPE_HAPTIC
                else -> SettingsManager.TYPE_MECHANICAL
            }
            if (settingsManager.soundEnabled) soundManager.play(sheet)
        }

        dialog.setContentView(sheet)
        dialog.show()
    }

    override fun onDestroy() {
        glowAnimator?.cancel()
        soundManager.release()
        super.onDestroy()
    }
}
