import api.Request;

void main(String[] args) throws Exception {
//    var request = new Request();
//    var session = request.createChatSession();
//
//    session.addMessage("user", "How many files are in the current directory?");
//
//    request.chat(session, (Runnable onUiUpdate) -> {
//        onUiUpdate.run();
//    });
//
//    System.out.println("Response: " + session.getCurrentTurn().getResponse());

    new ClaudeTerminalUI(new Request()).run();
}
