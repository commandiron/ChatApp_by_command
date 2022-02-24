package com.example.chatapp_by_command.view


import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.chatapp_by_command.domain.model.ChatMessage
import com.example.chatapp_by_command.presentation.bottomnavigation.BottomNavItem
import com.example.chatapp_by_command.presentation.chat.ChatViewModel
import com.example.chatapp_by_command.presentation.chat.components.*
import com.example.chatapp_by_command.presentation.chat.components.ChatInput
import com.example.chatapp_by_command.presentation.chat.components.chatrow.MessageStatus
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.SoftwareKeyboardController
import com.example.chatapp_by_command.core.SnackbarController
import com.example.chatapp_by_command.domain.model.MessageRegister
import com.example.chatapp_by_command.domain.model.MyUser
import com.example.chatapp_by_command.presentation.chat.ProfilePictureDialog
import com.example.chatapp_by_command.ui.theme.backgroundColor
import com.example.chatapp_by_command.ui.theme.primaryColor
import com.google.accompanist.insets.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChatScreen(
    chatRoomUUID: String,
    opponentUUID: String,
    registerUUID: String,
    chatViewModel: ChatViewModel = hiltViewModel(),
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    keyboardController: SoftwareKeyboardController) {

    //Set SnackBar
    val toastMessage = chatViewModel.toastMessage.value
    LaunchedEffect(key1 = toastMessage){
        if(toastMessage != ""){
            SnackbarController(this).showSnackbar(snackbarHostState,toastMessage, null)
        }
    }

    //Profil fotoğrafına tıklayınca fotoğrafın büyümesi gerekiyor.
    //Chat sırasında keyboard açıldığında son mesaj yukarı gelmiyor. Onu bir şekilde halletmem lazım.
    //Performans sorunu var, özellikle son mesaja scrollstate ile scroll etmeyi çalıştırınca bu sorun çıktı.

    chatViewModel.loadMessagesFromFirebase(chatRoomUUID, opponentUUID, registerUUID)

    ChatScreenContent(chatRoomUUID, opponentUUID, registerUUID, chatViewModel, navController, keyboardController)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ChatScreenContent(
    chatRoomUUID: String,
    opponentUUID: String,
    registerUUID: String,
    chatViewModel: ChatViewModel,
    navController: NavHostController,
    keyboardController: SoftwareKeyboardController) {



    val context = LocalContext.current
    val messages = chatViewModel.messages

    //Scroll Lazy Column //Bu kısım performansı düşürüyor.
    val scrollState = rememberLazyListState()
    val messagesLoadedFirstTime = chatViewModel.messagesLoadedFirstTime.value
    LaunchedEffect(key1 = messagesLoadedFirstTime, messages){
        if(messages.size > 0){
            scrollState.scrollToItem(
                index = messages.size - 1)
        }
    }
    val messageInserted = chatViewModel.messageInserted.value
    LaunchedEffect(key1 = messageInserted){
        if(messages.size > 0){
            scrollState.animateScrollToItem(
                index = messages.size - 1)
        }
    }

    //Load Oppoenent Profile
    LaunchedEffect(key1 = Unit){
        chatViewModel.loadOpponentProfileFromFirebase(opponentUUID)
    }
    var opponentProfileFromFirebase by remember {mutableStateOf(MyUser())}
    opponentProfileFromFirebase = chatViewModel.opponentProfileFromFirebase.value
    val opponentName = opponentProfileFromFirebase.userName
    val opponentSurname = opponentProfileFromFirebase.userSurName
    val opponentPictureUrl = opponentProfileFromFirebase.userProfilePictureUrl
    val opponentStatus = opponentProfileFromFirebase.status

    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        ProfilePictureDialog(opponentPictureUrl) {
            showDialog = !showDialog
        }
    }

    //Compose Components
    Column(
        modifier = Modifier
            .fillMaxSize()
            .focusable()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { keyboardController.hide() })
            }
            .background(Color(0xffFBE9E7))
    ) {

        ChatAppBar(
            title = "$opponentName $opponentSurname",
            description = opponentStatus.lowercase(),
            pictureUrl = opponentPictureUrl,
            onUserNameClick = {
                Toast.makeText(context, "User Profile Clicked", Toast.LENGTH_SHORT).show()
            }, onBackArrowClick = {
                navController.popBackStack()
                navController.navigate(BottomNavItem.UserList.screen_route)
            }, onUserProfilePictureClick = {
                showDialog = true
            }, onMorevertBlockUserClick = {
                chatViewModel.blockFriendToFirebase(registerUUID)
                navController.navigate(BottomNavItem.UserList.screen_route)
            }
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(backgroundColor),
            state = scrollState
        ) {
            items(messages) { message: MessageRegister ->

                val sdf = remember { SimpleDateFormat("hh:mm", Locale.ROOT) }

                when (message.isMessageFromOpponent){

                    true -> { //Opponent Message
                        ReceivedMessageRowAlt(
                            text = message.chatMessage.message,
                            opponentName = opponentName,
                            quotedMessage = null,
                            messageTime = sdf.format(message.chatMessage.date),
                        )
                    }

                    false ->{ //User Message
                        SentMessageRowAlt(
                            text = message.chatMessage.message,
                            quotedMessage = null,
                            messageTime = sdf.format(message.chatMessage.date),
                            messageStatus = MessageStatus.valueOf(message.chatMessage.status)
                        )
                    }

                }
            }
        }

        ChatInput(
            onMessageChange = { messageContent ->
                chatViewModel.insertMessageToFirebase(chatRoomUUID,messageContent,registerUUID)},
            modifier = Modifier.background(primaryColor)
        )
    }
}