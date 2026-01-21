package org.example;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

// Text Document Service Implementation
class SecaiTextDocumentService implements TextDocumentService {

    private static final Logger logger = Logger.getLogger(SecaiTextDocumentService.class.getName());

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
        return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        logger.info("Document opened: " + params.getTextDocument().getUri());
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        // Handle document changes if needed
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        logger.info("Document closed: " + params.getTextDocument().getUri());
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        logger.info("Document saved: " + params.getTextDocument().getUri());
    }
}
