// src/main/resources/static/js/dashboard.js
document.addEventListener('DOMContentLoaded', () => {
    const jobForm = document.getElementById('job-form');
    const formFeedback = document.getElementById('form-feedback');

    // Handle Job Submission
    jobForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const payload = document.getElementById('payload').value;
        const priority = document.getElementById('priority').value;

        try {
            const parsedPayload = JSON.parse(payload);
            const response = await fetch('/jobs', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ priority, payload: parsedPayload })
            });

            if (response.ok) {
                const result = await response.json();
                formFeedback.innerHTML = `<div class="alert alert-success">✅ Job enqueued with ID: ${result.jobId}</div>`;
                jobForm.reset();
            } else {
                formFeedback.innerHTML = `<div class="alert alert-danger">❌ Failed to enqueue job.</div>`;
            }
        } catch (error) {
            formFeedback.innerHTML = `<div class="alert alert-danger">❌ Invalid JSON payload.</div>`;
        }
    });

    // Function to update dashboard stats
    async function updateDashboard() {
        try {
            const response = await fetch('/api/v1/stats');
            const stats = await response.json();

            document.getElementById('queue-high-count').textContent = stats.high;
            document.getElementById('queue-normal-count').textContent = stats.normal;
            document.getElementById('queue-low-count').textContent = stats.low;
            document.getElementById('queue-processing-count').textContent = stats.processing;
            document.getElementById('queue-dlq-count').textContent = stats.dlq;

        } catch (error) {
            console.error('Failed to fetch stats:', error);
            document.getElementById('status-indicator').textContent = '● Error';
            document.getElementById('status-indicator').classList.replace('bg-success', 'bg-danger');
        }
    }

    // Initial update and then poll every 2 seconds
    updateDashboard();
    setInterval(updateDashboard, 2000);
});
