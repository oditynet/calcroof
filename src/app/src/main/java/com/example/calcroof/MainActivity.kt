package com.example.calcroof

import android.animation.ValueAnimator
import android.graphics.Matrix
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.calcroof.databinding.ActivityMainBinding
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.atan



class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val matrix = Matrix()

    // Текущие параметры трансформации
    private var currentScale = 1.08f
    private var currentTranslateX = 0f
    private var currentTranslateY = 0f

    private val minScale = 0.2f
    private val maxScale = 1.5f
    private val scaleStep = 0.03f
    private val moveStep = 50f

    // Для анимации
    private var scaleAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupImageControls()
        setupButton()
    }

    private fun setupImageControls() {
        // Увеличить масштаб с плавной анимацией
        binding.zoomInButton.setOnClickListener {
            val targetScale = (currentScale + scaleStep).coerceAtMost(maxScale)
            animateScale(currentScale, targetScale)
        }

        // Зажатие кнопки + для непрерывного увеличения
        binding.zoomInButton.setOnLongClickListener {
            val targetScale = maxScale
            animateScale(currentScale, targetScale)
            true
        }

        // Уменьшить масштаб с плавной анимацией
        binding.zoomOutButton.setOnClickListener {
            val targetScale = (currentScale - scaleStep).coerceAtLeast(minScale)
            animateScale(currentScale, targetScale)
        }

        // Зажатие кнопки - для непрерывного уменьшения
        binding.zoomOutButton.setOnLongClickListener {
            val targetScale = minScale
            animateScale(currentScale, targetScale)
            true
        }

        // Перемещение вверх
        binding.moveUpButton.setOnClickListener {
            currentTranslateY += moveStep
            applyTransformation()
        }

        // Перемещение вниз
        binding.moveDownButton.setOnClickListener {
            currentTranslateY -= moveStep
            applyTransformation()
        }

        // Перемещение влево
        binding.moveLeftButton.setOnClickListener {
            currentTranslateX += moveStep
            applyTransformation()
        }

        // Перемещение вправо
        binding.moveRightButton.setOnClickListener {
            currentTranslateX -= moveStep
            applyTransformation()
        }

        // Сброс изображения с плавной анимацией
        binding.resetImageButton.setOnClickListener {
            resetTransformation()
        }

        // Начальная загрузка
        binding.imageView.apply {
            scaleType = android.widget.ImageView.ScaleType.MATRIX
            post {
                fitImageToView()
            }
        }
    }

    private fun animateScale(fromScale: Float, toScale: Float) {
        // Отменяем предыдущую анимацию если есть
        scaleAnimator?.cancel()

        scaleAnimator = ValueAnimator.ofFloat(fromScale, toScale).apply {
            duration = 300 // Длительность анимации в мс
            interpolator = DecelerateInterpolator() // Плавное замедление

            addUpdateListener { animator ->
                val animatedScale = animator.animatedValue as Float
                currentScale = animatedScale
                applyTransformation()
            }

            start()
        }

        // Показываем прогресс
        val percent = (toScale * 100).toInt()
        // Можно добавить небольшой Toast при достижении границ
        if (toScale <= minScale) {
            Toast.makeText(this, "Минимальный масштаб: $percent%", Toast.LENGTH_SHORT).show()
        } else if (toScale >= maxScale) {
            Toast.makeText(this, "Максимальный масштаб: $percent%", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fitImageToView() {
        val imageView = binding.imageView
        val drawable = imageView.drawable ?: return

        val viewWidth = imageView.width.toFloat()
        val viewHeight = imageView.height.toFloat()
        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()

        if (viewWidth == 0f || viewHeight == 0f) return

        // Вычисляем масштаб чтобы поместить изображение
        val scaleX = viewWidth / drawableWidth
        val scaleY = viewHeight / drawableHeight
        val scale = minOf(scaleX, scaleY)

        // Центрируем
        val offsetX = (viewWidth - drawableWidth * scale) / 2
        val offsetY = (viewHeight - drawableHeight * scale) / 2

        // Анимируем возврат к исходному положению
        val startScale = currentScale
        val startTransX = currentTranslateX
        val startTransY = currentTranslateY

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 400
            interpolator = DecelerateInterpolator()

            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                currentScale = startScale + (scale - startScale) * fraction
                currentTranslateX = startTransX + (offsetX - startTransX) * fraction
                currentTranslateY = startTransY + (offsetY - startTransY) * fraction
                applyTransformation()
            }

            start()
        }
    }

    private fun applyTransformation() {
        matrix.reset()
        matrix.postScale(currentScale, currentScale)
        matrix.postTranslate(currentTranslateX, currentTranslateY)
        binding.imageView.imageMatrix = matrix
    }

    private fun resetTransformation() {
        fitImageToView()
        Toast.makeText(this, "Изображение сброшено", Toast.LENGTH_SHORT).show()
    }

    private fun setupButton() {
        binding.calculateButton.setOnClickListener {
            calculateResults()
        }
    }

    private fun calculateResults() {
        val angleStr = binding.angleInput.text.toString()
        val AStr = binding.inputA.text.toString()
        val CStr = binding.inputC.text.toString()
        val HStr = binding.inputH.text.toString()

        if (AStr.isEmpty() || CStr.isEmpty() || HStr.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val a = AStr.toDouble()
            val c = CStr.toDouble()
            val h = HStr.toDouble()

            // Вычисляем угол
            val angle: Double = if (angleStr.isNotEmpty()) {
                // Если угол задан - берем его
                angleStr.toDouble()
            } else {
                // Если не задан - вычисляем через арктангенс
                Math.toDegrees(atan(h / c ))
            }

            if (angle <= 0 || angle >= 90) {
                Toast.makeText(this, "Угол должен быть от 0° до 90°", Toast.LENGTH_SHORT).show()
                return
            }

            val angleInRadians = Math.toRadians(angle)
            val kResult = (a / 2) * tan(angleInRadians)
            val xResult = sqrt((a / 2) * (a / 2) + kResult * kResult)
            val tResult = sqrt(c * c + h * h)

            binding.result1.text = "1) Угол = %.2f°".format(angle)
            binding.result2.text = "2) k(2) = %.4f".format(kResult)
            binding.result3.text = "3) x(3) = √(%.4f) = %.4f".format(
                ((a / 2) * (a / 2) + kResult * kResult), xResult
            )
            binding.result4.text = "4) t(4) = √(%.2f² + %.2f²) = %.4f".format(c, h, tResult)

        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Пожалуйста, введите корректные числа", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка при вычислении: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}