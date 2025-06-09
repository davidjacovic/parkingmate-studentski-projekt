import React, { useState } from 'react';

function AddCommentForm({ parkingId, onNewComment }) {
    const [rating, setRating] = useState(5);
    const [reviewText, setReviewText] = useState('');
    const [error, setError] = useState('');
    const [submitting, setSubmitting] = useState(false);

    const handleSubmit = e => {
        e.preventDefault();
        setError('');

        const token = localStorage.getItem('token');

        if (rating < 1 || rating > 10) {
            setError('Ocena mora biti između 1 i 10.');
            return;
        }

        setSubmitting(true);

        fetch('http://localhost:3002/reviews', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
            },
            body: JSON.stringify({
                rating,
                review_text: reviewText,
                parking_location: parkingId,
            }),
        })
            .then(res => {
                if (!res.ok) throw new Error('Greška pri slanju komentara');
                return res.json();
            })
            .then(() => {
                onNewComment();
                setRating(5);
                setReviewText('');
            })
            .catch(() => {
                setError('Greška pri slanju komentara.');
            })
            .finally(() => setSubmitting(false));
    };

    return (
        <form
            onSubmit={handleSubmit}
            style={{
                marginTop: '1rem',
                backgroundColor: '#f0f4f8',
                padding: '1rem',
                borderRadius: '8px',
                boxShadow: '0 0 8px rgba(40, 75, 99, 0.15)'
            }}
        >
            <div style={{ marginBottom: '0.75rem' }}>
                <label
                    htmlFor="rating"
                    style={{ display: 'block', fontWeight: '600', color: '#284b63', marginBottom: '0.3rem' }}
                >
                    Ocena (1-10):
                </label>
                <input
                    id="rating"
                    type="number"
                    min="1"
                    max="10"
                    value={rating}
                    onChange={e => setRating(Number(e.target.value))}
                    required
                    style={{
                        width: '100%',
                        padding: '0.4rem 0.6rem',
                        fontSize: '1rem',
                        borderRadius: '4px',
                        border: '1px solid #284b63',
                        outlineColor: '#3c6e71',
                    }}
                />
            </div>

            <div style={{ marginBottom: '0.75rem' }}>
                <label
                    htmlFor="reviewText"
                    style={{ display: 'block', fontWeight: '600', color: '#284b63', marginBottom: '0.3rem' }}
                >
                    Komentar:
                </label>
                <textarea
                    id="reviewText"
                    value={reviewText}
                    onChange={e => setReviewText(e.target.value)}
                    rows="4"
                    required
                    style={{
                        width: '100%',
                        padding: '0.5rem',
                        fontSize: '1rem',
                        borderRadius: '4px',
                        border: '1px solid #284b63',
                        resize: 'vertical',
                        outlineColor: '#3c6e71',
                    }}
                />
            </div>

            <button
                type="submit"
                disabled={submitting}
                style={{
                    backgroundColor: '#284b63',
                    color: 'white',
                    padding: '0.6rem 1.2rem',
                    fontWeight: '600',
                    border: 'none',
                    borderRadius: '5px',
                    cursor: submitting ? 'not-allowed' : 'pointer',
                    transition: 'background-color 0.3s ease',
                }}
                onMouseEnter={e => {
                    if (!submitting) e.currentTarget.style.backgroundColor = '#3c6e71';
                }}
                onMouseLeave={e => {
                    if (!submitting) e.currentTarget.style.backgroundColor = '#284b63';
                }}
            >
                {submitting ? 'Šaljem...' : 'Dodaj komentar'}
            </button>

            {error && (
                <p
                    style={{
                        color: 'red',
                        marginTop: '0.8rem',
                        fontWeight: '600',
                        fontSize: '0.9rem',
                    }}
                >
                    {error}
                </p>
            )}
        </form>
    );
}

export default AddCommentForm;
