package dictionary.dictionaryandroid;

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dictionary.dictionaryandroid.R
import dictionary.dictionaryandroid.adapter.AudioAdapter
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    /**xml 변수 */
    private lateinit var audioRecordImageBtn: ImageButton
    private lateinit var myListImageBtn: ImageButton
    private lateinit var audioRecordText: TextView
    private lateinit var homeBtn: ImageButton

    /**오디오 파일 관련 변수 */
    // 오디오 권한
    private val recordPermission: String = Manifest.permission.RECORD_AUDIO
    private val PERMISSION_CODE = 21

    // 오디오 파일 녹음 관련 변수
    private var mediaRecorder: MediaRecorder? = null
    private var audioFileName: String? = null // 오디오 녹음 생성 파일 이름
    private var isRecording = false // 현재 녹음 상태를 확인하기 위함.
    private var audioUri: Uri? = null // 오디오 파일 uri

    // 오디오 파일 재생 관련 변수
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private lateinit var playIcon: ImageView

    /** 리사이클러뷰  */
    private var audioAdapter: AudioAdapter? = null
    private var audioList: ArrayList<Uri?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    // xml 변수 초기화
    // 리사이클러뷰 생성 및 클릭 이벤트
    private fun init() {
        audioRecordImageBtn = findViewById(R.id.record_image)
        audioRecordText = findViewById(R.id.record_text)
        myListImageBtn = findViewById(R.id.my_list_button)
        homeBtn = findViewById(R.id.home_btn)


        myListImageBtn.setOnClickListener(View.OnClickListener { View ->
            val intent = Intent(this, MyListActivity::class.java)
            startActivity(intent)
        })

        homeBtn.setOnClickListener(View.OnClickListener { View ->
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        })

        audioRecordImageBtn.setOnClickListener(View.OnClickListener { view ->
            if (isRecording) {
                // 녹음 중일 때의 동작
                isRecording = false
                audioRecordImageBtn.setBackgroundResource(R.drawable.record)
                audioRecordText.text = ""
                audioRecordText.setTextColor(R.drawable.record_red)
                audioRecordText.text = "녹음을 시작하시려면 위의 아이콘을 눌러주세요."

                stopRecording()
            } else {
                // 녹음 중이 아닐 때의 동작
                if (checkAudioPermission()) {
                    isRecording = true
                    audioRecordImageBtn.setBackgroundResource(R.drawable.record_green)
                    audioRecordText.text = "녹음중"

                    startRecording()
                }
            }
        })

        // 리사이클러뷰
        val audioRecyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        audioList = ArrayList()
        audioAdapter = AudioAdapter(this, audioList)
        audioRecyclerView.adapter = audioAdapter
        val mLayoutManager = LinearLayoutManager(this)
        mLayoutManager.orientation = LinearLayoutManager.VERTICAL
        audioRecyclerView.layoutManager = mLayoutManager

        // 커스텀 이벤트 리스너 4. 액티비티에서 커스텀 리스너 객체 생성 및 전달
        audioAdapter!!.setOnItemClickListener { view, position ->
            val uriName: String = java.lang.String.valueOf(audioList!![position])

            /*음성 녹화 파일에 대한 접근 변수 생성;
             * (ImageView)를 붙여줘서 View 객체를 형변환 시켜줌.
             * 전역변수로 한 이유는
             * */
            val file = File(uriName)
            if (isPlaying) {
                // 음성 녹화 파일이 여러개를 클릭했을 때 재생중인 파일의 Icon을 비활성화(비 재생중)으로 바꾸기 위함.
                if (playIcon === view as ImageView) {
                    // 같은 파일을 클릭했을 경우
                    stopAudio()
                } else {
                    // 다른 음성 파일을 클릭했을 경우
                    // 기존의 재생중인 파일 중지
                    stopAudio()

                    // 새로 파일 재생하기
                    playIcon = view as ImageView
                    playAudio(file)
                }
            } else {
                playIcon = view as ImageView
                playAudio(file)
            }
        }
    }

    // 오디오 파일 권한 체크
    private fun checkAudioPermission(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                applicationContext,
                recordPermission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(recordPermission), PERMISSION_CODE)
            false
        }
    }

    // 녹음 시작
    private fun startRecording() {
        // 파일의 외부 경로 확인
        val recordPath = getExternalFilesDir("/")!!.absolutePath
        // 파일 이름 변수를 현재 날짜가 들어가도록 초기화. 그 이유는 중복된 이름으로 기존에 있던 파일이 덮어 쓰여지는 것을 방지하고자 함.
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        audioFileName = recordPath + "/" +"RecordExample_" + timeStamp + "_"+"audio.mp4";

        // Media Recorder 생성 및 설정
        mediaRecorder = MediaRecorder()
        mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        mediaRecorder!!.setOutputFile(audioFileName)
        mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        try {
            mediaRecorder!!.prepare()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        // 녹음 시작
        mediaRecorder!!.start()
    }

    // 녹음 종료
    private fun stopRecording() {
        // 녹음 종료
        mediaRecorder!!.stop()
        mediaRecorder!!.release()
        mediaRecorder = null

        // 파일 경로(String) 값을 Uri로 변환해서 저장
        // - Why? : 리사이클러뷰에 들어가는 ArrayList가 Uri를 가지기 때문
        // - File Path를 알면 File을 인스턴스를 만들어 사용할 수 있기 때문
        audioUri = Uri.parse(audioFileName)

        // 데이터 ArrayList에 담기
        audioList!!.add(audioUri)

        // 데이터 갱신
        audioAdapter!!.notifyDataSetChanged()
    }

    // 녹음 파일 재생
    private fun playAudio(file: File) {
        mediaPlayer = MediaPlayer()
        try {
            mediaPlayer?.setDataSource(file.absolutePath)
            mediaPlayer?.prepare()
            mediaPlayer?.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
//        playIcon.setImageResource(R.drawable.record)
        isPlaying = true
        mediaPlayer?.setOnCompletionListener { stopAudio() }
    }

    // 녹음 파일 중지
    private fun stopAudio() {
//        playIcon.setImageResource(R.drawable.record)
        isPlaying = false
        mediaPlayer?.stop()
        mediaPlayer?.reset()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // 음성 인식 결과를 처리하는 함수
    private fun processSpeechRecognitionResult(result: String) {
        // 음성 인식 결과를 한국어로 번역하여 처리하는 기능을 추가하시면 됩니다.
        // 여기서는 번역하지 않고 결과를 그대로 출력합니다.
        Toast.makeText(this, "인식된 텍스트: $result", Toast.LENGTH_SHORT).show()
    }

    // 음성 인식 시작 함수
    private fun startSpeechRecognition() {
        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                // 음성 인식 준비 완료
            }

            override fun onBeginningOfSpeech() {
                // 음성 인식 시작
            }

            override fun onRmsChanged(rmsdB: Float) {
                // 음성 레벨 변경 (오디오 레벨 표시 등)
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // 녹음된 음성 데이터를 받음
            }

            override fun onEndOfSpeech() {
                // 음성 인식 종료
            }

            override fun onError(error: Int) {
                // 오류 발생시 처리 (오류 코드에 따라 다양한 오류 처리 가능)
            }

            override fun onResults(results: Bundle?) {
                // 음성 인식 결과 처리
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val recognizedText = matches[0]
                    processSpeechRecognitionResult(recognizedText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // 부분적인 음성 인식 결과를 처리 (실시간 인식에서 활용 가능)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // 이벤트 처리
            }
        })

        // 음성 인식 시작
        speechRecognizer.startListening(speechRecognizerIntent)

    }
}
