package com.example.savolator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.RecognizerIntent.*
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.savolator.ui.theme.SavolatorTheme
import com.example.speechrec2.SavoConverter.Companion.toSavo
import com.example.speechrec2.ShoppingListItem
import java.util.*

class MainActivity : ComponentActivity() {

    var outputTxt = mutableStateOf("Click button for Speech text ")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SavolatorTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android", outputTxt)
                }
            }
        }

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Greeting(name: String, outputTxt: MutableState<String>) {
    val context = LocalContext.current
    val ostoslista = remember { mutableStateListOf<ShoppingListItem>() }
    var textToSpeech: TextToSpeech? = null
    val openDialog = remember { mutableStateOf(false) }


    val rec = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            // if the condition is satisfied we are getting
            // the data from our string array list in our result.
            val result = it.data?.getStringArrayListExtra(EXTRA_RESULTS)
            // on below line we are setting result in our output text method.
            outputTxt.value = result?.get(0).toString()

            // "poista" to remove entry
            if (outputTxt.value.lowercase(Locale.getDefault()).startsWith("poista")
                && outputTxt.value.split(" ").size > 1
            ) {
                val itemToRemove = ShoppingListItem(outputTxt.value.split(" ")[1])
                if (! //ostoslista.contains(itemToRemove)) {
                    ostoslista.remove(itemToRemove)
                ) {
                    //}
                    //} else {
                    val item = ShoppingListItem(outputTxt.value)
                    // repeating existing value strikethrough it
                    if (ostoslista.contains(item)) {
                        val replacedItem = ShoppingListItem(outputTxt.value, true)
                        Collections.replaceAll(ostoslista, item, replacedItem)
                    } else {
                        ostoslista.add(item)
                    }
                    var retval = ""
                    outputTxt.value.split(" ").forEach {
                        retval += toSavo(it) + " " //TODO: process multiple words
                    }
                    textToSpeech = TextToSpeech(
                        context
                    ) {
                        if (it == TextToSpeech.SUCCESS) {
                            textToSpeech?.let {
                                it.language = Locale.getDefault()
                                it.setSpeechRate(1.0f)
                                it.speak(
                                    retval,
                                    TextToSpeech.QUEUE_FLUSH,//QUEUE_ADD,
                                    null,
                                    ""//null
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        Column(verticalArrangement = Arrangement.SpaceBetween) {
            LazyColumn(modifier = Modifier.fillMaxSize(0.9f)) {
                stickyHeader { Text("${ostoslista.size} ostosta") }  // TODO: still overlaps on scrolling

                items(ostoslista) { ostos -> MessageRow(ostos) }
            }
            Spacer(modifier = Modifier.fillMaxWidth())
            if (openDialog.value) {
                AlertDialog(
                    onDismissRequest = {
                        // Dismiss the dialog when the user clicks outside the dialog or on the back
                        // button. If you want to disable that functionality, simply use an empty
                        // onCloseRequest.
                        openDialog.value = false
                    },
                    title = { Text(text = "Varmistus") },
                    text = { Text("Oletko varma?") },
                    confirmButton = {
                        Button(onClick = { openDialog.value = false }) {
                            Text("Poista kaikki")
                            ostoslista.clear()
                        }
                    },
                    dismissButton = {
                        Button(onClick = { openDialog.value = false }) {
                            Text("Peru")
                        }
                    }
                )
            }
            Row(modifier = Modifier.weight(1f, false)) {

                /*Button(modifier = Modifier.padding(10.dp)
                    .pointerInput(Unit){detectTapGestures(onLongPress = {
                        openDialog.value=true
                    })},*/
                Box(modifier = Modifier.padding(5.dp)) {
                    Text(modifier = Modifier.combinedClickable(  // not possible with button
                        onClick = {
                            /*TODO: change this to actual */
                            if (ostoslista.last().collected) {
                                ostoslista.removeLast()
                            } else {
                                // to update list correctly
                                val temp = ostoslista.last()
                                temp.collected = true
                                ostoslista.removeLastOrNull()
                                ostoslista.add(temp)
                            }
                        },
                        onLongClick = { openDialog.value = true }
                    ), text = "Poista")
                }
                //) {  Text("Poista") }
                Button(modifier = Modifier.padding(10.dp), onClick = {
                    /*TODO*/
                    getSpeechInput(context = context, rec)
                    //ostoslista.add(ShoppingListItem("Ostos")) // for testing
                }) {
                    Text("Lisää")
                }
            }
            //Text(text = "Hello $name!")
        }
    }

    // this could be used to confirm long press = clear all
    @Composable
    fun SimpleAlertDialog(
        show: Boolean,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
    ) {
        if (show) {
            AlertDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(onClick = onConfirm)
                    { Text(text = "OK") }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss)
                    { Text(text = "Cancel") }
                },
                title = { Text(text = "Please confirm") },
                text = { Text(text = "Should I continue with the requested action?") }
            )
        }
    }


    // on below line we are creating a method
// to get the speech input from user.
    private fun getSpeechInput(
        context: Context,
        rec: ManagedActivityResultLauncher<Intent, ActivityResult>
    ) {
        // on below line we are checking if speech
        // recognizer intent is present or not.
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            // if the intent is not present we are simply displaying a toast message.
            Toast.makeText(context, "Speech not Available", Toast.LENGTH_SHORT).show()
        } else {
            // on below line we are calling a speech recognizer intent
            val intent = Intent(ACTION_RECOGNIZE_SPEECH)

            // on the below line we are specifying language model as language web search
            intent.putExtra( EXTRA_LANGUAGE_MODEL, LANGUAGE_MODEL_WEB_SEARCH )

            // on below line we are specifying extra language as default english language
            intent.putExtra(EXTRA_LANGUAGE, Locale.getDefault())

            // on below line we are specifying prompt as Speak something
            intent.putExtra(EXTRA_PROMPT, "Speak Something")

            // at last we are calling start activity for result to start our activity.
            rec.launch(intent)//startActivityForResult(intent, 101)
        }
    }

    @Composable
    fun MessageRow(item: ShoppingListItem) {
        if (item.collected) {
            Text(item.title, style = TextStyle(textDecoration = TextDecoration.LineThrough))
        } else {
            Text(item.title)
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        val s = remember { mutableStateOf("Click button for Speech text ") }
        SavolatorTheme {
            Greeting("Android", s)
        }
    }