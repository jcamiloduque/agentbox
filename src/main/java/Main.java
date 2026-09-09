import api.Request;

void main(String[] args) throws Exception {
    var request = new Request();
    var session = request.createChatSession();

    session.addMessage("user", "whjat fiels and what do they ahev and why in this folder");

    request.chat(session, (Runnable onUiUpdate) -> {
        onUiUpdate.run();
    });

    System.out.println("Response: " + session.getCurrentTurn().getResponse());

//    new ClaudeTerminalUI(new Request()).run();
}
