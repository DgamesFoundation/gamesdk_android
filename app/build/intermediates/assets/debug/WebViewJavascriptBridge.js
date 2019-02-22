
        (function() {
            if (window.WebViewJavascriptBridge) {
                return;
            }

            var messagingIframe;
            var sendMessageQueue = [];
            var receiveMessageQueue = [];
            var messageHandlers = {};

            var CUSTOM_PROTOCOL_SCHEME = 'yy';
            var QUEUE_HAS_MESSAGE = '__QUEUE_MESSAGE__/';

            var responseCallbacks = {};
            var uniqueId = 1;

            function _createQueueReadyIframe(doc) {
                //console.log("_createQueueReadyIframe");
                messagingIframe = doc.createElement('iframe');
                messagingIframe.style.display = 'none';
                doc.documentElement.appendChild(messagingIframe);
            }

            //set default messageHandler
            function init(messageHandler) {
                //console.log("init");
                if (WebViewJavascriptBridge._messageHandler) {
                    throw new Error('WebViewJavascriptBridge.init called twice');
                }
                WebViewJavascriptBridge._messageHandler = messageHandler;
                var receivedMessages = receiveMessageQueue;
                receiveMessageQueue = null;
                for (var i = 0; i < receivedMessages.length; i++) {
                    //console.log("init->receiveMessageQueue遍历:"+receivedMessages[i]);
                    _dispatchMessageFromNative(receivedMessages[i]);
                }
            }

            function send(data, responseCallback) {
                //console.log("send->data:"+data+"responseCallback:"+responseCallback);
                _doSend({
                    data: data
                }, responseCallback);
            }

            function registerHandler(handlerName, handler) {
                //console.log("registerHandler>handlerName:>"+handlerName);
                messageHandlers[handlerName] = handler;
                //console.log("registerHandler->messageHandlers长度:>"+messageHandlers.length);
            }

            function callHandler(handlerName, data, responseCallback) {
                //console.log("callHandler->"+"handlerName："+handlerName+"data："+data+"responseCallback："+responseCallback);
                _doSend({
                    handlerName: handlerName,
                    data: data
                }, responseCallback);
            }

            //sendMessage add message, 触发native处理 sendMessage
            function _doSend(message, responseCallback) {
                //console.log("_doSend"+message);
                if (responseCallback) {
                    var callbackId = 'cb_' + (uniqueId++) + '_' + new Date().getTime();
                    //console.log("_doSend->callbackId"+callbackId);
                    responseCallbacks[callbackId] = responseCallback;
                    //console.log("_doSend->responseCallbacks"+responseCallbacks.length);
                    message.callbackId = callbackId;
                    //console.log("_doSend->callbackId"+message);
                }

                sendMessageQueue.push(message);
                messagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://' + QUEUE_HAS_MESSAGE;
            }

            // 提供给native调用,该函数作用:获取sendMessageQueue返回给native,由于android不能直接获取返回的内容,所以使用url shouldOverrideUrlLoading 的方式返回内容
            function _fetchQueue() {
                //console.log("_fetchQueue");
                var messageQueueString = JSON.stringify(sendMessageQueue);
                sendMessageQueue = [];
                //android can't read directly the return data, so we can reload iframe src to communicate with java
                messagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://return/_fetchQueue/' + encodeURIComponent(messageQueueString);
            }


            //提供给native使用,
            function _dispatchMessageFromNative(messageJSON) {
                //console.log("_dispatchMessageFromNative"+messageJSON);
                setTimeout(function() {
                    var message = JSON.parse(messageJSON);
                    var responseCallback;
                    //java call finished, now need to call js callback function
                    if (message.responseId) {
                        responseCallback = responseCallbacks[message.responseId];
                        if (!responseCallback) {
                            return;
                        }
                        responseCallback(message.responseData);
                        delete responseCallbacks[message.responseId];
                    } else {
                        //直接发送
                        if (message.callbackId) {
                            var callbackResponseId = message.callbackId;
                            responseCallback = function(responseData) {
                                _doSend({
                                    responseId: callbackResponseId,
                                    responseData: responseData
                                });
                            };
                        }

                        var handler = WebViewJavascriptBridge._messageHandler;
                        if (message.handlerName) {
                            handler = messageHandlers[message.handlerName];
                        }
                        //查找指定handler
                        try {
                            handler(message.data, responseCallback);
                        } catch (exception) {
                            if (typeof console != 'undefined') {
                                //console.log("WebViewJavascriptBridge: WARNING: javascript handler threw.", message, exception);
                            }
                        }
                    }
                });
            }

            //提供给native调用,receiveMessageQueue 在会在页面加载完后赋值为null,所以
            function _handleMessageFromNative(messageJSON) {
                //console.log(messageJSON);
                if (receiveMessageQueue && receiveMessageQueue.length > 0) {
                    receiveMessageQueue.push(messageJSON);
                    //console.log(" receiveMessageQueue.push("+messageJSON+")");
                } else {
                    _dispatchMessageFromNative(messageJSON);
                    //console.log(" _dispatchMessageFromNative("+messageJSON+")");
                }
            }
             function getHandlerAndHandle(handlerName, data) {
//                     var responseCallback = function (reponseToNative) {
//                         return reponseToNative;
//                     }
                       var responseDataH;
                    //console.log("js中getHandlerAndHandle被调用");
                     var handler1 = WebViewJavascriptBridge._messageHandler;
                     if (handlerName) {
                         handler1 = messageHandlers[handlerName];
                     }
                     //查找指定handler
                     try {
                         handler1(data, function (reponseToNative) {
                         responseDataH = reponseToNative;
                                             //console.log(reponseToNative);
                                              return reponseToNative;
                                                             });
                         return responseDataH;
                     }
                     catch (exception) {
                         if (typeof console != 'undefined') {
                             //console.log("WebViewJavascriptBridge: WARNING: javascript handler threw.", data, exception);
                         }

                     }

                 }
            var WebViewJavascriptBridge = window.WebViewJavascriptBridge = {
                init: init,
                send: send,
                registerHandler: registerHandler,
                callHandler: callHandler,
                _fetchQueue: _fetchQueue,
                getHandlerAndHandle:getHandlerAndHandle,
                _handleMessageFromNative: _handleMessageFromNative
            };

            var doc = document;
            _createQueueReadyIframe(doc);
            var readyEvent = doc.createEvent('Events');
            readyEvent.initEvent('WebViewJavascriptBridgeReady');
            readyEvent.bridge = WebViewJavascriptBridge;
            doc.dispatchEvent(readyEvent);
        })();
