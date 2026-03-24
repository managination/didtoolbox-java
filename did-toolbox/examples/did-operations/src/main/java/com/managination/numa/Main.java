package com.managination.numa;

import ch.admin.bj.swiyu.didtoolbox.model.TdwDidLogMetaPeeker;
import ch.admin.bj.swiyu.didtoolbox.model.WebVerifiableHistoryDidLogMetaPeeker;
import ch.admin.eid.didresolver.Did;

import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
   public static void main(String... args) {
      try {
         String log = Files.readString(Path.of("/Users/micharoon/projects/swiyu/didtoolbox-java/managination/didlog.jsonl")).replaceAll("\\r?\\n\\s*", "");
// Detect DID ID from the log
         String id = WebVerifiableHistoryDidLogMetaPeeker.peek(log).getDidDoc().getId();

// Perform full verification of all entries and proofs
         // Use the resolver again directly if needed, but resolveAll requires a DID log in JSONL format.
         // However, DidLogMeta already contains the resolved DID document and parameters if needed.
         // To make new Did(id).resolveAll(log) work with multi-line JSON, we'd need to normalize it here too if the library requires it.
         new Did(id).resolveAll(log);

         System.out.println("\n✅ Verification SUCCESSFUL: The DID log is correctly formatted and all proofs are valid.");
      } catch (Exception e) {
         System.err.println("\n❌ Verification FAILED: " + e.getMessage());
         if (e.getCause() != null) System.err.println("   Cause: " + e.getCause().getMessage());
      }
   }
}
