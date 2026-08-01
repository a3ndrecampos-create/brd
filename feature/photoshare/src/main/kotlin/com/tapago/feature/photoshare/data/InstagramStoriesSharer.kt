package com.tapago.feature.photoshare.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import com.tapago.core.common.Outcome
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import android.graphics.Bitmap

private const val INSTAGRAM_PACKAGE = "com.instagram.android"
private const val ADD_TO_STORY_ACTION = "com.instagram.share.ADD_TO_STORY"

/**
 * Encapsula o fluxo de compartilhamento nativo para o Instagram Stories.
 * Se o Instagram não estiver instalado, cai no share sheet padrão do Android
 * como fallback (WhatsApp, e-mail, etc.).
 */
class InstagramStoriesSharer @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Salva o bitmap final em cache e devolve o content:// URI via FileProvider. */
    fun saveToCache(bitmap: Bitmap): Outcome<Uri> = try {
        val file = File(context.cacheDir, "tapago_share_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        Outcome.Success(uri)
    } catch (e: Exception) {
        Outcome.Error(e, "Não foi possível preparar a imagem para compartilhamento")
    }

    fun isInstagramInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(INSTAGRAM_PACKAGE, PackageManager.GET_ACTIVITIES)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /**
     * Abre o Instagram Stories já com a imagem (foto + overlay) como
     * background do story, pronta para postar. Requer que a imagem já
     * tenha sido salva via [saveToCache].
     */
    fun shareToStories(imageUri: Uri): Outcome<Unit> {
        if (!isInstagramInstalled()) {
            return shareViaGenericSheet(imageUri)
        }
        return try {
            val intent = Intent(ADD_TO_STORY_ACTION).apply {
                setDataAndType(imageUri, "image/*")
                putExtra("interactive_asset_uri", imageUri)
                setPackage(INSTAGRAM_PACKAGE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Outcome.Success(Unit)
        } catch (e: Exception) {
            shareViaGenericSheet(imageUri)
        }
    }

    private fun shareViaGenericSheet(imageUri: Uri): Outcome<Unit> = try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar corrida").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        Outcome.Success(Unit)
    } catch (e: Exception) {
        Outcome.Error(e, "Não foi possível abrir o compartilhamento")
    }
}
