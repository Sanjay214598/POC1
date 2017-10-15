"use strict";

// --------
// WARNING:
// --------

// THIS CODE IS ONLY MADE AVAILABLE FOR DEMONSTRATION PURPOSES AND IS NOT SECURE!
// DO NOT USE IN PRODUCTION!

// FOR SECURITY REASONS, USING A JAVASCRIPT WEB APP HOSTED VIA THE CORDA NODE IS
// NOT THE RECOMMENDED WAY TO INTERFACE WITH CORDA NODES! HOWEVER, FOR THIS
// PRE-ALPHA RELEASE IT'S A USEFUL WAY TO EXPERIMENT WITH THE PLATFORM AS IT ALLOWS
// YOU TO QUICKLY BUILD A UI FOR DEMONSTRATION PURPOSES.

// GOING FORWARD WE RECOMMEND IMPLEMENTING A STANDALONE WEB SERVER THAT AUTHORISES
// VIA THE NODE'S RPC INTERFACE. IN THE COMING WEEKS WE'LL WRITE A TUTORIAL ON
// HOW BEST TO DO THIS.

const app = angular.module('demoAppModule', ['ui.bootstrap']);

// Fix for unhandled rejections bug.
app.config(['$qProvider', function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);

app.controller('DemoAppController', function($http, $location, $uibModal) {
    const demoApp = this;

    // We identify the node.
    const apiBaseURL = "/api/counterparty/";
    let peers = [];

    $http.get(apiBaseURL + "me").then((response) => demoApp.thisNode = response.data.me);

    $http.get(apiBaseURL + "peers").then((response) => peers = response.data.peers);

    demoApp.openModal = () => {
        const modalInstance = $uibModal.open({
            templateUrl: 'demoAppModal.html',
            controller: 'ModalInstanceCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                apiBaseURL: () => apiBaseURL,
                peers: () => peers
            }
        });

        modalInstance.result.then(() => {}, () => {});
    };

    demoApp.getAs = () => $http.get(apiBaseURL + "contracts/CounterPartyB")
        .then((response) => demoApp.ious = Object.keys(response.data)
            .map((key) => response.data[key].state.data)
            .reverse());
    demoApp.getIOUs = function(){
                $http.get(apiBaseURL + "contracts/"+demoApp.thisNode)
                .then(function(response) {
                    demoApp.ious = Object.keys(response.data).map((key) => response.data[key].state.data)
                })
                ;
            }


    demoApp.getIOUs();
});

app.controller('ModalInstanceCtrl', function ($http, $location, $uibModalInstance, $uibModal, apiBaseURL, peers) {
    const modalInstance = this;

    modalInstance.peers = peers;
    modalInstance.form = {};
    modalInstance.formError = false;

    // Validate and create IOU.
    modalInstance.create = () => {
        if (invalidFormInput()) {
            modalInstance.formError = true;
        } else {
            modalInstance.formError = false;

            const iou = {
                value: modalInstance.form.value,
				address: modalInstance.form.address,
				eligibleCollateral: modalInstance.form.eligibleCollateral,
				interestCashCollateral: modalInstance.form.interestCashCollateral,
				threshold: modalInstance.form.threshold,
				mta: modalInstance.form.mta,
				initialMarginCollateral: modalInstance.form.initialMarginCollateral,
				variationMarginCollateral: modalInstance.form.variationMarginCollateral,
				comments: modalInstance.form.comments
            };

            $uibModalInstance.close();

            const createIOUEndpoint =
                apiBaseURL +
                modalInstance.form.counterparty +
                "/create-iou";

            // Create PO and handle success / fail responses.
            $http.put(createIOUEndpoint, angular.toJson(iou)).then(
                (result) => modalInstance.displayMessage(result),
                (result) => modalInstance.displayMessage(result)
            );
        }
    };

    modalInstance.displayMessage = (message) => {
        const modalInstanceTwo = $uibModal.open({
            templateUrl: 'messageContent.html',
            controller: 'messageCtrl',
            controllerAs: 'modalInstanceTwo',
            resolve: { message: () => message }
        });

        // No behaviour on close / dismiss.
        modalInstanceTwo.result.then(() => {}, () => {});
    };

    // Close create IOU modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

    // Validate the IOU.
    function invalidFormInput() {
        return (modalInstance.form.counterparty === undefined);
    }
});

// Controller for success/fail modal dialogue.
app.controller('messageCtrl', function ($uibModalInstance, message) {
    const modalInstanceTwo = this;
    modalInstanceTwo.message = message.data;
});