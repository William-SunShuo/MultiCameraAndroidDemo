package com.blink.monitor.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.children
import com.blink.monitor.databinding.ScoreCountViewBinding
import com.blink.monitor.extention.gone
import com.blink.monitor.extention.onClick
import com.blink.monitor.extention.visible

@SuppressLint("SetTextI18n")
class ScoreCountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var gameType: GameType = GameType.FootBall

    private var binding: ScoreCountViewBinding =
        ScoreCountViewBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        with(binding) {

            updateUI(gameType)

            if(gamePart.visibility == View.VISIBLE) {
                gamePart.children.forEach { it ->
                    it.onClick {
                        gamePart.children.forEach { view ->
                            view.isSelected = (view == it)
                        }
                    }
                }
            }

            addHomeScore.onClick {
                homeScore.text = (homeScore.text.toString().toInt() + 1).toString()
            }
            delHomeScore.onClick {
                var score = homeScore.text.toString().toInt() - 1
                if (score < 0) score = 0
                homeScore.text = (score).toString()
            }

            addOtherScore.onClick {
                otherScore.text = (otherScore.text.toString().toInt() + 1).toString()
            }
            delOtherScore.onClick {
                var score = otherScore.text.toString().toInt() - 1
                if (score < 0) score = 0
                otherScore.text = (score).toString()
            }
        }
    }


    //主队积分;
    fun getHomeTeamScore(): Int {
        return binding.homeScore.text.toString().toInt()
    }

    //客队积分
    fun getAwayTeamScore(): Int {
        return binding.otherTeam.text.toString().toInt()
    }

    //设置比赛模式
    fun setGameType(type: GameType) {
        this.gameType = type
        updateUI(type)
    }

    enum class GameType {
        FootBall, Puck, BasketBall
    }


    private fun updateUI(gameType: GameType) {
        with(binding) {
            when (gameType) {

                //足球
                GameType.FootBall -> gamePart.gone()

                //冰球
                GameType.Puck -> {
                    gamePart.visible()
                }

                //篮球
                GameType.BasketBall -> {
                    gamePart.visible()
                    fourPart.visible()
                }
            }
        }
    }

}