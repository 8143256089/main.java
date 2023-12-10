import (
	"encoding/json"
	"fmt"
	"net/http"
	"sort"
	"sync"
)

// SortRequest represents the structure of the incoming JSON request.
type SortRequest struct {
	Numbers []int `json:"numbers"`
}

// SortResponse represents the structure of the JSON response.
type SortResponse struct {
	SortedNumbers []int `json:"sortedNumbers"`
}

func sortSingleHandler(w http.ResponseWriter, r *http.Request) {
	// Decode the incoming JSON request
	var req SortRequest
	err := json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		http.Error(w, "Invalid JSON request", http.StatusBadRequest)
		return
	}

	// Sort the array
	sort.Ints(req.Numbers)

	// Prepare the response
	resp := SortResponse{SortedNumbers: req.Numbers}

	// Encode and send the response
	w.Header().Set("Content-Type", "application/json")
	err = json.NewEncoder(w).Encode(resp)
	if err != nil {
		http.Error(w, "Error encoding JSON response", http.StatusInternalServerError)
		return
	}
}

func sortConcurrentHandler(w http.ResponseWriter, r *http.Request) {
	// Decode the incoming JSON request
	var reqs []SortRequest
	err := json.NewDecoder(r.Body).Decode(&reqs)
	if err != nil {
		http.Error(w, "Invalid JSON request", http.StatusBadRequest)
		return
	}

	// Use a wait group to wait for all goroutines to finish
	var wg sync.WaitGroup

	// Channel to collect sorted arrays
	results := make(chan SortResponse, len(reqs))

	// Process each array concurrently
	for _, req := range reqs {
		wg.Add(1)
		go func(arr []int) {
			defer wg.Done()

			// Sort the array
			sort.Ints(arr)

			// Send the sorted array to the results channel
			results <- SortResponse{SortedNumbers: arr}
		}(req.Numbers)
	}

	// Close the results channel once all goroutines are done
	go func() {
		wg.Wait()
		close(results)
	}()

	// Collect all sorted arrays from the results channel
	var sortedArrays []SortResponse
	for result := range results {
		sortedArrays = append(sortedArrays, result)
	}

	// Prepare the response
	resp := map[string]interface{}{
		"sortedArrays": sortedArrays,
	}

	// Encode and send the response
	w.Header().Set("Content-Type", "application/json")
	err = json.NewEncoder(w).Encode(resp)
	if err != nil {
		http.Error(w, "Error encoding JSON response", http.StatusInternalServerError)
		return
	}
}

func main() {
	// Define the routes and handlers
	http.HandleFunc("/sort/single", sortSingleHandler)
	http.HandleFunc("/sort/concurrent", sortConcurrentHandler)

	// Start the server on port 8080
	port := 8080
	fmt.Printf("Server is running on :%d...\n", port)
	err := http.ListenAndServe(fmt.Sprintf(":%d", port), nil)
	if err != nil {
		fmt.Println("Error starting the server:", err)
	}
}
