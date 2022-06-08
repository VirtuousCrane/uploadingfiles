package com.example.uploadingfiles;

import com.example.uploadingfiles.storage.StorageFileNotFoundException;
import com.example.uploadingfiles.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.stream.Collectors;

@Controller
public class FileUploadController {

    private final StorageService storageService;

    @Autowired
    public FileUploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Finds and return the link to all the uploaded files
     *
     * @param model
     * @return The file upload page
     * @throws IOException
     */
    @GetMapping("/")
    public String listUploadedFiles(Model model) throws IOException {
        model.addAttribute("files", storageService.loadAll().map(
                // For each path returned by the storageService apply, get the filename and call
                // serveFile
                path -> MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
                            "serveFile",
                            path.getFileName().toString())
                        .build()
                        .toUri()
                        .toString())
                .collect(Collectors.toList()));

        return "uploadForm";
    }

    /**
     * Given the link to a file, return both the html embedding (as header) and the file itself
     * @param filename
     * @return The response entity with the requested file attached to it
     */
    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Resource file = storageService.loadAsResource(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }

    /**
     * Stores the uploaded file and redirects the user to /
     * @param file
     * @param redirectAttributes
     * @return
     */
    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        storageService.store(file);
        redirectAttributes.addFlashAttribute("message", "You successfully uploaded " + file.getOriginalFilename() + "!");
        return "redirect:/";
    }

    /**
     * Yeets an exception when a file could not be found
     * @param exc
     * @return
     */
    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

}
