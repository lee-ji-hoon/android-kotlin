package com.guide.geoGuiz

import android.content.Intent
import android.os.Build.*
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelProvider
import com.guide.geoGuiz.databinding.ActivityMainBinding

private const val TAG = "MainActivity"
private const val KEY_INDEX = "index"

class MainActivity : AppCompatActivity() {
    private lateinit var mainBinding: ActivityMainBinding // 컴파일 시점에 초기화 되는 것은 불가능하기 때문에 lateinit
    private lateinit var getResult: ActivityResultLauncher<Intent>
    private val correctQuestions = mutableSetOf<Int>()

    private val quizViewModel: QuizViewModel by lazy {
        ViewModelProvider(this)[QuizViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "create")
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        quizViewModel.currentIndex = savedInstanceState?.getInt(KEY_INDEX, 0) ?: 0
        updateQuestion()

        mainBinding.btnTrue.setOnClickListener {
            checkAnswer(true)
        }

        mainBinding.btnFalse.setOnClickListener {
            checkAnswer(false)
        }

        mainBinding.btnNext.setOnClickListener {
            updateNextQuestion()
        }

        // 챌린지 1. TextView에 리스너추가
        mainBinding.tvQuestion.setOnClickListener {
            updateNextQuestion()
        }

        // 챌린지 2. PREVIOUS 버튼 추가
        mainBinding.btnPrevious.setOnClickListener {
            updatePrevQuestion()
        }


        // startActivityForResult가 deprecated됐기 때문에 대체할 방법인 ActivityResultLauncher 사용
        getResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
                if (activityResult.resultCode == RESULT_OK) {
                    val answerData =
                        activityResult.data?.getBooleanExtra(EXTRA_ANSWER_SHOWN, false) ?: false
                    if (answerData) {
                        quizViewModel.useCheat()
                        changeCheatCount()
                    }
                }
            }

        mainBinding.btnCheat.setOnClickListener { view ->
            when (isUseCheat()) {
                true -> startCheatIntent(view)
                false -> {
                    Toast.makeText(this, getString(R.string.unable_cheeat), Toast.LENGTH_SHORT).show()
                    mainBinding.btnCheat.isEnabled = false
                }
            }
        }
        changeCheatCount()
    }

    /**
     * cheatCount 변경
     */
    private fun changeCheatCount() {
        mainBinding.tvCheat.text = quizViewModel.tvCheatCount
    }

    /**
     * CheatAcivity 시작
     */
    private fun startCheatIntent(view: View) {
        val answerIsTrue = quizViewModel.currentQuestionAnswer
        val intent = CheatActivity.newIntent(this@MainActivity, answerIsTrue)
        // android.app.ActivityOptions이전 버전과 호환되는 방식으로 기능에 액세스하기 위한 도우미
        val options =
            ActivityOptionsCompat.makeClipRevealAnimation(view, 0, 0, view.width, view.height) // 뷰 객체, x, y, 새 액티비티 너비와 높이
        if (VERSION.SDK_INT >= VERSION_CODES.M) getResult.launch(intent, options)
        else getResult.launch(intent)
    }

    /**
     * 치트를 사용할 수 있는지 확인
     */
    private fun isUseCheat(): Boolean {
        val cheatCount = quizViewModel.cheatCount
        if (cheatCount >= MAX_CHEAT_COUNT) return false
        return true
    }

    /*override fun onStart() {
        super.onStart()
        Log.d(TAG, "start")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "resume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "pause")
    }

    override fun onSaveInstanceState(savedInstacneState: Bundle) {
        super.onSaveInstanceState(savedInstacneState)
        Log.d(TAG, "onSaveInstanceState")
        savedInstacneState.putInt(KEY_INDEX, quizViewModel.currentIndex)
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "stop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "destroy")
    }*/

    /**
     * 버튼 상태 갱신
     */
    private fun changedButtonEnabled(enabled: Boolean) {
        mainBinding.btnTrue.isEnabled = enabled
        mainBinding.btnFalse.isEnabled = enabled
    }

    /**
     * 이전 질문으로 돌아가기
     */
    private fun updatePrevQuestion() {
        if (quizViewModel.currentIndex == 0) quizViewModel.moveToLast()
        else quizViewModel.moveToPrev()
        updateQuestion()
    }

    /**
     * 다음 질문으로 가기
     */
    private fun updateNextQuestion() {
        quizViewModel.moveToNext()
        updateQuestion()
    }

    /**
     * 질문 업데이트
     */
    private fun updateQuestion() {
        // 챌린지 1. 정답 맞춘 문제를 건너띄기
        isOverlapQuestion()
        val questionTextResId = quizViewModel.currentQuestionId
        mainBinding.tvQuestion.setText(questionTextResId)
    }

    /**
     * 이미 푼 문제인지 확인하는 함수
     */
    private fun isOverlapQuestion() {
        val currentQuestionId = quizViewModel.currentQuestionId
        when (correctQuestions.contains(currentQuestionId)) {
            // stack에 존재한다면 false -> 버튼 클릭 못하게 바꿈
            true -> changedButtonEnabled(false)
            false -> changedButtonEnabled(true)
        }
    }

    /**
     * 정답 확인
     */
    private fun checkAnswer(userAnswer: Boolean) {
        val correctAnswer = quizViewModel.currentQuestionAnswer
        // 챌린지 2. 점수 보여주기. -> 마지막 문제인지 확인하고 마지막이면 정답 체크 후 점수를 백분율로 보여주기.
        val messageResId = when {
            // 만약 현재 문제를 컨닝 했을 경우
            quizViewModel.currentQuestionIsCheat -> R.string.judgement_toast
            userAnswer == correctAnswer -> {
                correctQuestions.add(quizViewModel.currentQuestionId)
                changedButtonEnabled(false)
                R.string.correct_toast
            }
            else -> R.string.inCorrect_toast
        }
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT)
            .show()
        if (messageResId != R.string.judgement_toast) checkLastQuestion()
    }

    /**
     * 마지막 문제에서 몇 개 맞췄는지 확인
     */
    private fun checkLastQuestion() {
        if (quizViewModel.currentIndex == quizViewModel.questionBoxSize - 1) {
            val ratioCorrectQuestion =
                ((correctQuestions.size.toDouble() / quizViewModel.questionBoxSize.toDouble()) * 100.0).toInt()
            Toast.makeText(
                this,
                getString(
                    R.string.ratio_correct,
                    ratioCorrectQuestion,
                    quizViewModel.getCheatCount
                ),
                Toast.LENGTH_SHORT
            ).show()
        } else updateNextQuestion()
    }
}