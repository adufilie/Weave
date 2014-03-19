
analysis_mod.controller("RunQueryCtrl", function($scope, queryService) {
			
		$scope.runQuery = function(){
			queryHandler = new aws.QueryHandler(queryService.queryObject);
          
            // doesn't work to remove weave instance -> $scope.weaveInstancePanel = "";
            // Probably need to put a broadcast event here? to tell weave instance panel to die.
			queryHandler.runQuery();
		};
		
		$scope.updateVisualizations = function(){
			if(queryHandler) {
				queryHandler.updateVisualizations(queryService.queryObject);
			}
		};
		
		$scope.clearWeave = function(){
			if (queryHandler != undefined) {
				queryHandler.clearWeave();
			}
		};
});