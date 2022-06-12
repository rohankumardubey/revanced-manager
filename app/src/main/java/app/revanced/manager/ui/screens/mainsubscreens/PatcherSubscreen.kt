package app.revanced.manager.ui.screens.mainsubscreens


import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.net.toFile
import androidx.navigation.NavController
import app.revanced.manager.R
import app.revanced.manager.backend.vital.ApkUtil
import app.revanced.manager.ui.screens.destinations.AppSelectorScreenDestination
import app.revanced.manager.ui.screens.destinations.PatchesSelectorScreenDestination
import app.revanced.patcher.PatcherOptions
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import java.io.File
import app.revanced.manager.backend.utils.patcher.addPatchesFiltered
import app.revanced.manager.backend.utils.patcher.applyPatchesVerbose
import app.revanced.manager.backend.utils.patcher.mergeFiles

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@RootNavGraph
@Composable
// patcher_subscreen
fun PatcherSubscreen(
    navigator: NavController,
    resultRecipient: ResultRecipient<AppSelectorScreenDestination, String>
) {
    var selectedAppPackage by rememberSaveable { mutableStateOf("") }
    val sela = stringResource(id = R.string.card_application_body)
    val selb = stringResource(id = R.string.card_application_body_selected)
    var e = ""
    val pm = ApkUtil(LocalContext.current)

    val applications = pm.getInstalledApplications()
    resultRecipient.onNavResult { result ->
        when (result) {
            is NavResult.Canceled -> {}
            is NavResult.Value -> {
                selectedAppPackage = result.value

                println(pm.pathFromPackageName(packageName = result.value))
            }
        }
    }
    var g : File? = null
    val pickintegrations = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { file ->
        if (file != null) {
            g = file.toFile()
            // Update the state with the Uri
        }
    }
    Scaffold(floatingActionButton = {
        ExtendedFloatingActionButton(
            onClick = {
                var patcher = app.revanced.patcher.Patcher(PatcherOptions(File(pm.pathFromPackageName(selectedAppPackage)), pm.getCacheDir().toString(), false))

                pickintegrations.launch("application/java-archive")
                if (g != null) {

                patcher.addFiles(listOf(g) as List<File>)
                }
                // add patches, but filter incompatible or excluded patches
                patcher.addPatchesFiltered(includeFilter = includedPatches.isNotEmpty())
                // apply patches
                patcher.applyPatchesVerbose()

                // write output file
                if (output.exists()) Files.delete(output.toPath())
                MainCommand.inputFile.copyTo(output)

                ZipFileSystemUtils(output).use { fileSystem ->
                    // replace all dex files
                    val result = patcher.save()
                    result.dexFiles.forEach {
                        fileSystem.write(it.name, it.memoryDataStore.data)
                    }

                    // write resources
                    if (!disableResourcePatching) {
                        fileSystem.writePathRecursively(File(cacheDirectory).resolve("build").toPath())
                        fileSystem.uncompress(*result.doNotCompress!!.toTypedArray())
                    }
                }
                      },
            icon = { Icon(imageVector = Icons.Default.Build, contentDescription = "sd") },
            text = { Text(text = "Patch") })
    }) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth(),
                onClick = {
                    navigator.navigate(
                        AppSelectorScreenDestination(applications, arrayOf("aboba")).route
                    )
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.card_application_header),
                        style = MaterialTheme.typography.titleMedium
                    )
                    when (selectedAppPackage) {
                        "" -> {
                            e = sela
                        }
                        else -> {
                            e = selectedAppPackage
                        }
                    }
                    Text(
                        text = e,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(0.dp, 8.dp)
                    )
                }
            }
            Card(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth(),
                onClick = {
                    navigator.navigate(
                        PatchesSelectorScreenDestination(
                            arrayOf("patch-1", "patch-2", "patch-3"),
                            arrayOf("aboba")
                        ).route
                    )
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.card_patches_header),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(id = R.string.card_patches_body_patches),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(0.dp, 8.dp)
                    )
                }
            }

        }
    }

}

@Preview
@Composable
fun PatcherSubscreenPreview() {
    // PatcherSubscreen()
}